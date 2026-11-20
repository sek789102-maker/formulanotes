import 'dart:convert';
import 'dart:isolate';
import 'dart:math';
import 'dart:typed_data';

import 'package:crypto/crypto.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:local_auth/local_auth.dart';

import '../core/l10n/lang.dart';
import '../core/utils/errors.dart';
import '../core/utils/validators.dart';

/// Minimal key/value interface over secure storage (fakeable in tests).
abstract class SecureKV {
  Future<String?> read(String key);
  Future<void> write(String key, String value);
  Future<void> delete(String key);
}

class FlutterSecureKV implements SecureKV {
  FlutterSecureKV() : _s = const FlutterSecureStorage();
  final FlutterSecureStorage _s;

  @override
  Future<String?> read(String key) => _s.read(key: key);
  @override
  Future<void> write(String key, String value) => _s.write(key: key, value: value);
  @override
  Future<void> delete(String key) => _s.delete(key: key);
}

class MemorySecureKV implements SecureKV {
  final Map<String, String> data = {};
  @override
  Future<String?> read(String key) async => data[key];
  @override
  Future<void> write(String key, String value) async => data[key] = value;
  @override
  Future<void> delete(String key) async => data.remove(key);
}

enum PinCheck { ok, wrong, lockedOut }

class PinResult {
  const PinResult(this.check, {this.lockedFor});
  final PinCheck check;
  final Duration? lockedFor;
}

/// PIN + biometric app lock.
///
/// The PIN is never stored: only a salted PBKDF2-HMAC-SHA256 hash, kept in
/// Android Keystore-backed secure storage. Repeated failures trigger an
/// increasing lockout.
class SecurityService {
  SecurityService(this.kv, {LocalAuthentication? auth, this.iterations = 60000}) : _auth = auth; // ignore: prefer_initializing_formals

  final SecureKV kv;
  final LocalAuthentication? _auth;
  final int iterations;

  static const _kPin = 'pin_hash_v1';
  static const _kLock = 'lock_enabled';
  static const _kBio = 'biometric_enabled';
  static const _kFails = 'pin_failures';
  static const _kLockUntil = 'pin_lock_until';
  static const freeAttempts = 5;

  Future<bool> isLockEnabled() async => (await kv.read(_kLock)) == 'true' && await hasPin();
  Future<bool> hasPin() async => (await kv.read(_kPin)) != null;
  Future<bool> isBiometricEnabled() async => (await kv.read(_kBio)) == 'true';

  Future<void> setBiometricEnabled(bool v) => kv.write(_kBio, v ? 'true' : 'false');

  /// Sets a new PIN and enables the lock.
  Future<void> setPin(String pin) async {
    if (!Validators.isValidPin(pin)) {
      throw AppException(ErrorKind.validation, Lang.t('PIN ต้องเป็นตัวเลข 4–6 หลัก', 'PIN must be 4–6 digits'));
    }
    final salt = Uint8List.fromList(List.generate(16, (_) => Random.secure().nextInt(256)));
    final hash = await _derive(pin, salt, iterations);
    await kv.write(
      _kPin,
      jsonEncode({'v': 1, 'len': pin.length, 'iter': iterations, 'salt': base64Encode(salt), 'hash': base64Encode(hash)}),
    );
    await kv.write(_kLock, 'true');
    await kv.delete(_kFails);
    await kv.delete(_kLockUntil);
  }

  Future<void> disableLock() async {
    await kv.delete(_kPin);
    await kv.write(_kLock, 'false');
    await kv.write(_kBio, 'false');
    await kv.delete(_kFails);
    await kv.delete(_kLockUntil);
  }

  /// Number of digits of the stored PIN (used to auto-submit exactly once).
  Future<int> pinLength() async {
    final raw = await kv.read(_kPin);
    if (raw == null) return 6;
    try {
      return ((jsonDecode(raw) as Map)['len'] as num?)?.toInt() ?? 6;
    } catch (_) {
      return 6;
    }
  }

  Future<Duration?> remainingLockout({DateTime? now}) async {
    final until = int.tryParse(await kv.read(_kLockUntil) ?? '');
    if (until == null) return null;
    final diff = DateTime.fromMillisecondsSinceEpoch(until).difference(now ?? DateTime.now());
    return diff.isNegative ? null : diff;
  }

  Future<PinResult> verifyPin(String pin, {DateTime? now}) async {
    final n = now ?? DateTime.now();
    final locked = await remainingLockout(now: n);
    if (locked != null) return PinResult(PinCheck.lockedOut, lockedFor: locked);

    final raw = await kv.read(_kPin);
    if (raw == null) return const PinResult(PinCheck.wrong);
    final m = jsonDecode(raw) as Map<String, dynamic>;
    final salt = base64Decode(m['salt'] as String);
    final expected = base64Decode(m['hash'] as String);
    final iter = (m['iter'] as num).toInt();
    final actual = await _derive(pin, salt, iter);

    if (constantTimeEquals(actual, expected)) {
      await kv.delete(_kFails);
      await kv.delete(_kLockUntil);
      return const PinResult(PinCheck.ok);
    }
    final fails = (int.tryParse(await kv.read(_kFails) ?? '') ?? 0) + 1;
    await kv.write(_kFails, '$fails');
    if (fails >= freeAttempts) {
      final secs = min(300, 30 * (fails - freeAttempts + 1));
      final until = n.add(Duration(seconds: secs));
      await kv.write(_kLockUntil, '${until.millisecondsSinceEpoch}');
      return PinResult(PinCheck.lockedOut, lockedFor: Duration(seconds: secs));
    }
    return const PinResult(PinCheck.wrong);
  }

  static bool constantTimeEquals(List<int> a, List<int> b) {
    if (a.length != b.length) return false;
    var r = 0;
    for (var i = 0; i < a.length; i++) {
      r |= a[i] ^ b[i];
    }
    return r == 0;
  }

  static Future<Uint8List> _derive(String pin, Uint8List salt, int iterations) {
    // Runs off the UI thread to keep the PIN pad responsive.
    return Isolate.run(() => pbkdf2(utf8.encode(pin), salt, iterations, 32));
  }

  /// PBKDF2 with HMAC-SHA256 (RFC 8018).
  static Uint8List pbkdf2(List<int> password, List<int> salt, int iterations, int keyLength) {
    final hmac = Hmac(sha256, password);
    final out = BytesBuilder();
    var block = 1;
    while (out.length < keyLength) {
      final s = Uint8List(salt.length + 4)
        ..setAll(0, salt)
        ..setAll(salt.length, [block >> 24 & 0xff, block >> 16 & 0xff, block >> 8 & 0xff, block & 0xff]);
      var u = hmac.convert(s).bytes;
      final t = Uint8List.fromList(u);
      for (var i = 1; i < iterations; i++) {
        u = hmac.convert(u).bytes;
        for (var j = 0; j < t.length; j++) {
          t[j] ^= u[j];
        }
      }
      out.add(t);
      block++;
    }
    return Uint8List.fromList(out.toBytes().sublist(0, keyLength));
  }

  // ---------- biometrics ----------

  LocalAuthentication get _la => _auth ?? LocalAuthentication();

  Future<bool> biometricsAvailable() async {
    try {
      if (!await _la.isDeviceSupported()) return false;
      if (!await _la.canCheckBiometrics) return false;
      return (await _la.getAvailableBiometrics()).isNotEmpty;
    } catch (e) {
      if (kDebugMode) debugPrint('biometrics check failed: $e');
      return false;
    }
  }

  Future<bool> authenticateBiometric() async {
    try {
      return await _la.authenticate(
        localizedReason: Lang.t('ยืนยันตัวตนเพื่อเปิด LIFEKEEP', 'Authenticate to open LIFEKEEP'),
        biometricOnly: true,
        persistAcrossBackgrounding: true,
      );
    } catch (e) {
      if (kDebugMode) debugPrint('biometric auth failed: $e');
      return false;
    }
  }
}
