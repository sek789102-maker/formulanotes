import 'package:flutter_test/flutter_test.dart';
import 'package:lifekeep/core/utils/errors.dart';
import 'package:lifekeep/services/security_service.dart';

String hex(List<int> b) => b.map((e) => e.toRadixString(16).padLeft(2, '0')).join();

void main() {
  test('PBKDF2-HMAC-SHA256 matches published test vectors', () {
    expect(
      hex(SecurityService.pbkdf2('password'.codeUnits, 'salt'.codeUnits, 1, 32)),
      '120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b',
    );
    expect(
      hex(SecurityService.pbkdf2('password'.codeUnits, 'salt'.codeUnits, 2, 32)),
      'ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43',
    );
    expect(
      hex(SecurityService.pbkdf2('password'.codeUnits, 'salt'.codeUnits, 4096, 32)),
      'c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134a',
    );
  });

  test('PIN is stored only as salted hash and verifies correctly', () async {
    final kv = MemorySecureKV();
    final s = SecurityService(kv, iterations: 500);
    await s.setPin('482915');
    expect(await s.isLockEnabled(), isTrue);
    expect(await s.pinLength(), 6);
    expect(kv.data.values.any((v) => v.contains('482915')), isFalse);
    expect((await s.verifyPin('482915')).check, PinCheck.ok);
    expect((await s.verifyPin('000000')).check, PinCheck.wrong);

    // Same PIN, new salt -> different stored hash.
    final first = kv.data['pin_hash_v1'];
    await s.setPin('482915');
    expect(kv.data['pin_hash_v1'], isNot(first));
  });

  test('lockout after repeated failures, then succeeds after waiting', () async {
    final s = SecurityService(MemorySecureKV(), iterations: 200);
    await s.setPin('1234');
    final t = DateTime(2026, 11, 20, 12);
    for (var i = 0; i < SecurityService.freeAttempts - 1; i++) {
      expect((await s.verifyPin('9999', now: t)).check, PinCheck.wrong);
    }
    final locked = await s.verifyPin('9999', now: t);
    expect(locked.check, PinCheck.lockedOut);
    expect(locked.lockedFor, const Duration(seconds: 30));
    // Even the right PIN is refused during lockout.
    expect((await s.verifyPin('1234', now: t.add(const Duration(seconds: 10)))).check, PinCheck.lockedOut);
    expect((await s.verifyPin('1234', now: t.add(const Duration(seconds: 31)))).check, PinCheck.ok);
  });

  test('invalid PINs are rejected; disabling clears everything', () async {
    final s = SecurityService(MemorySecureKV(), iterations: 100);
    await expectLater(s.setPin('12'), throwsA(isA<AppException>()));
    await expectLater(s.setPin('12a4'), throwsA(isA<AppException>()));
    await s.setPin('1234');
    await s.setBiometricEnabled(true);
    await s.disableLock();
    expect(await s.isLockEnabled(), isFalse);
    expect(await s.hasPin(), isFalse);
    expect(await s.isBiometricEnabled(), isFalse);
  });

  test('constant time compare', () {
    expect(SecurityService.constantTimeEquals([1, 2, 3], [1, 2, 3]), isTrue);
    expect(SecurityService.constantTimeEquals([1, 2, 3], [1, 2, 4]), isFalse);
    expect(SecurityService.constantTimeEquals([1, 2], [1, 2, 3]), isFalse);
  });
}
