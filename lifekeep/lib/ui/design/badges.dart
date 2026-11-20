import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';
import '../../core/theme/tokens.dart';

/// Compact rounded label (status, category, counts).
class Pill extends StatelessWidget {
  const Pill({
    super.key,
    required this.label,
    required this.color,
    required this.background,
    this.icon,
    this.dense = false,
  });
  final String label;
  final Color color;
  final Color background;
  final IconData? icon;
  final bool dense;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: dense ? 6 : 8, vertical: dense ? 1 : 3),
      decoration: BoxDecoration(color: background, borderRadius: BorderRadius.circular(Radii.xs)),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[Icon(icon, size: dense ? 12 : 13, color: color), const SizedBox(width: 3)],
          Flexible(
            child: Text(
              label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontFamily: AppTheme.fontFamily,
                color: color,
                fontSize: dense ? 11 : 11.5,
                height: 1.35,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// Tinted rounded-square icon used for categories and settings rows.
class IconBadge extends StatelessWidget {
  const IconBadge({
    super.key,
    required this.icon,
    required this.color,
    this.size = Sizes.iconBadge,
    this.filled = false,
  });
  final IconData icon;
  final Color color;
  final double size;

  /// Solid background with white glyph (settings style).
  final bool filled;

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    final bg = filled ? color : color.withValues(alpha: dark ? 0.22 : 0.11);
    final fg = filled ? Colors.white : (dark ? Color.lerp(color, Colors.white, 0.5)! : color);
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(size * 0.3)),
      child: Icon(icon, size: size * 0.52, color: fg),
    );
  }
}

/// Small numeric badge (tab counters).
class CountBadge extends StatelessWidget {
  const CountBadge({super.key, required this.count, this.alert = false, this.selected = false});
  final int count;
  final bool alert;
  final bool selected;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final bg = alert ? context.status.danger : (selected ? scheme.primary : scheme.surfaceContainerHighest);
    final fg = alert ? Colors.white : (selected ? scheme.onPrimary : scheme.onSurfaceVariant);
    return AnimatedContainer(
      duration: Motion.fast,
      constraints: const BoxConstraints(minWidth: 18),
      padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
      decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(Radii.pill)),
      child: Text(
        '$count',
        textAlign: TextAlign.center,
        style: TextStyle(
          fontFamily: AppTheme.fontFamily,
          fontSize: 11,
          height: 1.35,
          fontWeight: FontWeight.w700,
          color: fg,
        ),
      ),
    );
  }
}

class StatusDot extends StatelessWidget {
  const StatusDot({super.key, required this.color, this.size = 8});
  final Color color;
  final double size;

  @override
  Widget build(BuildContext context) => Container(
    width: size,
    height: size,
    decoration: BoxDecoration(color: color, shape: BoxShape.circle),
  );
}
