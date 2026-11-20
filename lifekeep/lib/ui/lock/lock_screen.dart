import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../services/security_service.dart';
import '../../state/app_controller.dart';
import 'pin_pad.dart';

class LockScreen extends StatefulWidget {
  const LockScreen({super.key, required this.onUnlocked});
  final VoidCallback onUnlocked;

  @override
  State<LockScreen> createState() => _LockScreenState();
}

class _LockScreenState extends State<LockScreen> {
  String _pin = '';
  int _len = 6;
  bool _busy = false;
  bool _error = false;
  String? _message;
  Duration? _lockout;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      final c = context.read<AppController>();
      final len = await c.s.security.pinLength();
      if (mounted) setState(() => _len = len);
      await _checkLockout();
      if (c.biometricEnabled && _lockout == null) _biometric();
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  Future<void> _checkLockout() async {
    final c = context.read<AppController>();
    final l = await c.s.security.remainingLockout();
    if (!mounted) return;
    _startCountdown(l);
  }

  void _startCountdown(Duration? l) {
    _timer?.cancel();
    setState(() => _lockout = l);
    if (l == null) return;
    _timer = Timer.periodic(const Duration(seconds: 1), (t) {
      final next = (_lockout ?? Duration.zero) - const Duration(seconds: 1);
      if (!mounted) return;
      if (next <= Duration.zero) {
        t.cancel();
        setState(() {
          _lockout = null;
          _message = null;
        });
      } else {
        setState(() => _lockout = next);
      }
    });
  }

  Future<void> _biometric() async {
    final c = context.read<AppController>();
    final ok = await c.s.security.authenticateBiometric();
    if (ok && mounted) widget.onUnlocked();
  }

  Future<void> _onDigit(String d) async {
    if (_busy || _lockout != null || _pin.length >= _len) return;
    setState(() {
      _pin += d;
      _error = false;
      _message = null;
    });
    if (_pin.length == _len) await _trySubmit();
  }

  Future<void> _trySubmit() async {
    final c = context.read<AppController>();
    setState(() => _busy = true);
    final r = await c.s.security.verifyPin(_pin);
    if (!mounted) return;
    switch (r.check) {
      case PinCheck.ok:
        setState(() => _busy = false);
        widget.onUnlocked();
        return;
      case PinCheck.lockedOut:
        setState(() {
          _busy = false;
          _pin = '';
          _error = true;
          _message = context.tr('ใส่ PIN ผิดหลายครั้ง กรุณารอสักครู่', 'Too many attempts. Please wait.');
        });
        _startCountdown(r.lockedFor);
        return;
      case PinCheck.wrong:
        setState(() {
          _busy = false;
          _pin = '';
          _error = true;
          _message = context.tr('PIN ไม่ถูกต้อง', 'Incorrect PIN');
        });
    }
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;
    final c = context.watch<AppController>();
    return Material(
      color: Theme.of(context).scaffoldBackgroundColor,
      child: SafeArea(
        child: LayoutBuilder(
          builder: (context, box) => SingleChildScrollView(
            child: ConstrainedBox(
              constraints: BoxConstraints(minHeight: box.maxHeight),
              child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                const SizedBox(height: 24),
                Container(
                  width: 64,
                  height: 64,
                  decoration: BoxDecoration(color: scheme.primary, borderRadius: BorderRadius.circular(20)),
                  child: Icon(Icons.lock_outline_rounded, color: scheme.onPrimary, size: 30),
                ),
                const SizedBox(height: 16),
                Text('LIFEKEEP', style: text.titleLarge?.copyWith(letterSpacing: 2)),
                const SizedBox(height: 6),
                Text(context.tr('ใส่ PIN เพื่อปลดล็อก', 'Enter PIN to unlock'),
                    style: text.bodyMedium?.copyWith(color: scheme.onSurfaceVariant)),
                SizedBox(
                  height: 40,
                  child: Center(
                    child: Text(
                      _lockout != null
                          ? context.tr('ลองใหม่ได้ใน ${_lockout!.inSeconds} วินาที', 'Try again in ${_lockout!.inSeconds}s')
                          : (_message ?? ''),
                      style: TextStyle(color: scheme.error),
                    ),
                  ),
                ),
                PinPad(
                  length: _pin.length,
                  maxLength: _len,
                  error: _error,
                  enabled: !_busy && _lockout == null,
                  onDigit: _onDigit,
                  onDelete: () => setState(() {
                    if (_pin.isNotEmpty) _pin = _pin.substring(0, _pin.length - 1);
                    _error = false;
                  }),
                  onBiometric: c.biometricEnabled ? _biometric : null,
                ),
                const SizedBox(height: 24),
              ]),
            ),
          ),
        ),
      ),
    );
  }
}
