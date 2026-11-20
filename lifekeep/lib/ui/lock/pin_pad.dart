import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// Numeric keypad with PIN dots. Does not use a text field, so no keyboard
/// suggestions or clipboard can capture the PIN.
class PinPad extends StatelessWidget {
  const PinPad({
    super.key,
    required this.length,
    required this.maxLength,
    required this.onDigit,
    required this.onDelete,
    this.onBiometric,
    this.enabled = true,
    this.error = false,
  });

  final int length;
  final int maxLength;
  final ValueChanged<String> onDigit;
  final VoidCallback onDelete;
  final VoidCallback? onBiometric;
  final bool enabled;
  final bool error;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    Widget key(String d) => _Key(
      enabled: enabled,
      onTap: () {
        HapticFeedback.selectionClick();
        onDigit(d);
      },
      child: Text(d, style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w500)),
    );
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            for (var i = 0; i < maxLength; i++)
              AnimatedContainer(
                duration: const Duration(milliseconds: 150),
                margin: const EdgeInsets.symmetric(horizontal: 8),
                width: 14,
                height: 14,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: error ? scheme.error : (i < length ? scheme.primary : Colors.transparent),
                  border: Border.all(color: error ? scheme.error : scheme.outline, width: 1.5),
                ),
              ),
          ],
        ),
        const SizedBox(height: 32),
        for (final row in const [
          ['1', '2', '3'],
          ['4', '5', '6'],
          ['7', '8', '9'],
        ])
          Row(mainAxisAlignment: MainAxisAlignment.center, children: [for (final d in row) key(d)]),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            onBiometric == null
                ? const SizedBox(width: 88, height: 72)
                : _Key(
                    enabled: enabled,
                    onTap: onBiometric!,
                    child: Icon(Icons.fingerprint_rounded, size: 30, color: scheme.primary),
                  ),
            key('0'),
            _Key(
              enabled: enabled,
              onTap: onDelete,
              child: Icon(Icons.backspace_outlined, color: scheme.onSurfaceVariant),
            ),
          ],
        ),
      ],
    );
  }
}

class _Key extends StatelessWidget {
  const _Key({required this.child, required this.onTap, required this.enabled});
  final Widget child;
  final VoidCallback onTap;
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(6),
      child: SizedBox(
        width: 76,
        height: 60,
        child: Material(
          color: Colors.transparent,
          shape: const StadiumBorder(),
          clipBehavior: Clip.antiAlias,
          child: InkWell(
            onTap: enabled ? onTap : null,
            child: Center(child: child),
          ),
        ),
      ),
    );
  }
}
