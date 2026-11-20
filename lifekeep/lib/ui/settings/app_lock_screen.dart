import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/theme/app_theme.dart';
import '../../services/security_service.dart';
import '../../state/app_controller.dart';
import '../lock/pin_pad.dart';
import '../widgets/common.dart';

class AppLockSettingsScreen extends StatefulWidget {
  const AppLockSettingsScreen({super.key});

  @override
  State<AppLockSettingsScreen> createState() => _AppLockSettingsScreenState();
}

class _AppLockSettingsScreenState extends State<AppLockSettingsScreen> {
  bool _bioAvailable = false;

  @override
  void initState() {
    super.initState();
    context.read<AppController>().s.security.biometricsAvailable().then((v) {
      if (mounted) setState(() => _bioAvailable = v);
    });
  }

  Future<void> _enable() async {
    final c = context.read<AppController>();
    final pin = await Navigator.push<String>(context, MaterialPageRoute(builder: (_) => const PinSetupScreen()));
    if (pin == null || !mounted) return;
    await runAction(context, () async {
      await c.s.security.setPin(pin);
      await c.refreshSecurity();
    }, success: context.tr('เปิดล็อกแอปแล้ว', 'App lock enabled'));
  }

  Future<void> _change() async {
    final c = context.read<AppController>();
    final ok = await Navigator.push<bool>(context, MaterialPageRoute(builder: (_) => const PinVerifyScreen()));
    if (ok != true || !mounted) return;
    final pin = await Navigator.push<String>(context, MaterialPageRoute(builder: (_) => const PinSetupScreen()));
    if (pin == null || !mounted) return;
    final bio = c.biometricEnabled;
    await runAction(context, () async {
      await c.s.security.setPin(pin);
      await c.s.security.setBiometricEnabled(bio);
      await c.refreshSecurity();
    }, success: context.tr('เปลี่ยน PIN แล้ว', 'PIN changed'));
  }

  Future<void> _disable() async {
    final c = context.read<AppController>();
    final ok = await Navigator.push<bool>(context, MaterialPageRoute(builder: (_) => const PinVerifyScreen()));
    if (ok != true || !mounted) return;
    await runAction(context, () async {
      await c.s.security.disableLock();
      await c.refreshSecurity();
    }, success: context.tr('ปิดล็อกแอปแล้ว', 'App lock disabled'));
  }

  Future<void> _toggleBio(bool v) async {
    final c = context.read<AppController>();
    if (v) {
      // Confirm the sensor works before enabling it.
      final ok = await c.s.security.authenticateBiometric();
      if (!ok) {
        if (mounted) showSnack(context, context.tr('ยืนยันลายนิ้วมือไม่สำเร็จ', 'Biometric check failed'), error: true);
        return;
      }
    }
    await c.s.security.setBiometricEnabled(v);
    await c.refreshSecurity();
  }

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: Text(context.tr('ล็อกแอป', 'App lock'))),
      body: ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 32), children: [
        AppCard(
          child: Row(children: [
            IconBadge(icon: c.lockEnabled ? Icons.lock_rounded : Icons.lock_open_rounded, color: scheme.primary, size: 48),
            const SizedBox(width: 14),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(c.lockEnabled ? context.tr('ล็อกแอปเปิดอยู่', 'App lock is on') : context.tr('ล็อกแอปปิดอยู่', 'App lock is off'),
                    style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 4),
                Text(
                  context.tr('ต้องใส่ PIN ทุกครั้งที่เปิดแอปหรือกลับเข้าแอป และซ่อนเนื้อหาในหน้าแอปล่าสุด',
                      'PIN required whenever the app opens or returns, and content is hidden in Recent Apps'),
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
                ),
              ]),
            ),
          ]),
        ),
        const SizedBox(height: 16),
        if (!c.lockEnabled)
          FilledButton.icon(onPressed: _enable, icon: const Icon(Icons.pin_outlined), label: Text(context.tr('ตั้ง PIN และเปิดล็อก', 'Set PIN & enable')))
        else
          Card(
            child: Column(children: [
              SwitchListTile(
                secondary: const Icon(Icons.fingerprint_rounded),
                title: Text(context.tr('ปลดล็อกด้วยลายนิ้วมือ / ใบหน้า', 'Unlock with biometrics')),
                subtitle: Text(_bioAvailable
                    ? context.tr('ใช้ข้อมูลชีวมิติที่ลงทะเบียนในเครื่อง', 'Uses biometrics enrolled on this device')
                    : context.tr('อุปกรณ์ไม่รองรับหรือยังไม่ได้ลงทะเบียน', 'Not supported or not enrolled on this device')),
                value: c.biometricEnabled,
                onChanged: _bioAvailable ? _toggleBio : null,
              ),
              const Divider(indent: 56),
              ListTile(leading: const Icon(Icons.password_rounded), title: Text(context.tr('เปลี่ยน PIN', 'Change PIN')), onTap: _change),
              const Divider(indent: 56),
              ListTile(
                leading: Icon(Icons.lock_open_rounded, color: context.status.danger),
                title: Text(context.tr('ปิดล็อกแอป', 'Disable app lock'), style: TextStyle(color: context.status.danger)),
                onTap: _disable,
              ),
            ]),
          ),
        const SizedBox(height: 16),
        Text(
          context.tr(
            'PIN ไม่ถูกเก็บเป็นข้อความ ระบบเก็บเฉพาะค่าแฮช (PBKDF2-SHA256 + salt) ใน Secure Storage ของเครื่อง '
                'หากลืม PIN จะไม่สามารถกู้คืนได้ ต้องล้างข้อมูลแอป ดังนั้นควรสำรองข้อมูลไว้เสมอ',
            'Your PIN is never stored as text — only a salted PBKDF2-SHA256 hash in the device\'s secure storage. '
                'A forgotten PIN cannot be recovered (app data would need to be cleared), so keep backups.',
          ),
          style: Theme.of(context).textTheme.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
        ),
      ]),
    );
  }
}

/// Two-step PIN creation (enter + confirm). Pops with the PIN.
class PinSetupScreen extends StatefulWidget {
  const PinSetupScreen({super.key});

  @override
  State<PinSetupScreen> createState() => _PinSetupScreenState();
}

class _PinSetupScreenState extends State<PinSetupScreen> {
  String _first = '';
  String _pin = '';
  bool _confirming = false;
  bool _error = false;

  void _next() {
    if (!_confirming) {
      if (_pin.length < 4) return;
      setState(() {
        _first = _pin;
        _pin = '';
        _confirming = true;
      });
      return;
    }
    if (_pin == _first) {
      Navigator.pop(context, _pin);
    } else {
      setState(() {
        _error = true;
        _pin = '';
        _first = '';
        _confirming = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final max = _confirming ? _first.length : 6;
    return Scaffold(
      appBar: AppBar(title: Text(context.tr('ตั้ง PIN', 'Set PIN'))),
      body: SafeArea(
        child: SingleChildScrollView(
          child: Column(children: [
            const SizedBox(height: 24),
            Text(
              _confirming ? context.tr('ยืนยัน PIN อีกครั้ง', 'Confirm your PIN') : context.tr('ตั้ง PIN 4–6 หลัก', 'Choose a 4–6 digit PIN'),
              style: Theme.of(context).textTheme.titleLarge,
            ),
            SizedBox(
              height: 36,
              child: Center(
                child: Text(
                  _error ? context.tr('PIN ไม่ตรงกัน กรุณาตั้งใหม่', 'PINs did not match. Try again.') : '',
                  style: TextStyle(color: scheme.error),
                ),
              ),
            ),
            PinPad(
              length: _pin.length,
              maxLength: max,
              onDigit: (d) {
                if (_pin.length >= max) return;
                setState(() {
                  _pin += d;
                  _error = false;
                });
                if (_confirming && _pin.length == _first.length) _next();
              },
              onDelete: () => setState(() {
                if (_pin.isNotEmpty) _pin = _pin.substring(0, _pin.length - 1);
              }),
            ),
            const SizedBox(height: 16),
            if (!_confirming)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 32),
                child: FilledButton(
                  onPressed: _pin.length >= 4 ? _next : null,
                  child: Text(context.tr('ถัดไป', 'Next')),
                ),
              ),
            const SizedBox(height: 24),
          ]),
        ),
      ),
    );
  }
}

/// Asks for the current PIN. Pops with true when correct.
class PinVerifyScreen extends StatefulWidget {
  const PinVerifyScreen({super.key});

  @override
  State<PinVerifyScreen> createState() => _PinVerifyScreenState();
}

class _PinVerifyScreenState extends State<PinVerifyScreen> {
  String _pin = '';
  int _len = 6;
  bool _busy = false;
  String? _msg;
  Timer? _timer;
  Duration? _lockout;

  @override
  void initState() {
    super.initState();
    final s = context.read<AppController>().s.security;
    s.pinLength().then((l) {
      if (mounted) setState(() => _len = l);
    });
    s.remainingLockout().then(_countdown);
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void _countdown(Duration? d) {
    _timer?.cancel();
    if (!mounted) return;
    setState(() => _lockout = d);
    if (d == null) return;
    _timer = Timer.periodic(const Duration(seconds: 1), (t) {
      if (!mounted) return;
      final n = (_lockout ?? Duration.zero) - const Duration(seconds: 1);
      setState(() => _lockout = n <= Duration.zero ? null : n);
      if (n <= Duration.zero) t.cancel();
    });
  }

  Future<void> _submit() async {
    final s = context.read<AppController>().s.security;
    setState(() => _busy = true);
    final r = await s.verifyPin(_pin);
    if (!mounted) return;
    setState(() {
      _busy = false;
      _pin = '';
    });
    switch (r.check) {
      case PinCheck.ok:
        Navigator.pop(context, true);
      case PinCheck.wrong:
        setState(() => _msg = context.tr('PIN ไม่ถูกต้อง', 'Incorrect PIN'));
      case PinCheck.lockedOut:
        setState(() => _msg = context.tr('ใส่ผิดหลายครั้ง กรุณารอ', 'Too many attempts, please wait'));
        _countdown(r.lockedFor);
    }
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: Text(context.tr('ยืนยัน PIN ปัจจุบัน', 'Enter current PIN'))),
      body: SafeArea(
        child: SingleChildScrollView(
          child: Column(children: [
            const SizedBox(height: 24),
            Text(context.tr('ใส่ PIN ปัจจุบัน', 'Enter your current PIN'), style: Theme.of(context).textTheme.titleLarge),
            SizedBox(
              height: 36,
              child: Center(
                child: Text(
                  _lockout != null ? context.tr('ลองใหม่ได้ใน ${_lockout!.inSeconds} วินาที', 'Try again in ${_lockout!.inSeconds}s') : (_msg ?? ''),
                  style: TextStyle(color: scheme.error),
                ),
              ),
            ),
            PinPad(
              length: _pin.length,
              maxLength: _len,
              enabled: !_busy && _lockout == null,
              error: _msg != null && _pin.isEmpty,
              onDigit: (d) {
                if (_pin.length >= _len) return;
                setState(() {
                  _pin += d;
                  _msg = null;
                });
                if (_pin.length == _len) _submit();
              },
              onDelete: () => setState(() {
                if (_pin.isNotEmpty) _pin = _pin.substring(0, _pin.length - 1);
              }),
            ),
          ]),
        ),
      ),
    );
  }
}
