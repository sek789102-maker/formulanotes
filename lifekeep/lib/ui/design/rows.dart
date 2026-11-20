import 'package:flutter/material.dart';

import '../../core/theme/tokens.dart';

/// The standard list row used inside [LkGroup]s across the app.
class LkRow extends StatelessWidget {
  const LkRow({
    super.key,
    required this.title,
    this.subtitle,
    this.leading,
    this.trailing,
    this.onTap,
    this.onLongPress,
    this.titleStyle,
    this.showChevron = false,
    this.dense = false,
    this.titleMaxLines = 1,
  });

  final String title;
  final Widget? subtitle;
  final Widget? leading;
  final Widget? trailing;
  final VoidCallback? onTap;
  final VoidCallback? onLongPress;
  final TextStyle? titleStyle;
  final bool showChevron;
  final bool dense;
  final int titleMaxLines;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    final scheme = Theme.of(context).colorScheme;
    return InkWell(
      onTap: onTap,
      onLongPress: onLongPress,
      child: ConstrainedBox(
        constraints: BoxConstraints(minHeight: dense ? 48 : Sizes.row),
        child: Padding(
          padding: EdgeInsets.symmetric(horizontal: Space.lg, vertical: dense ? Space.sm : 10),
          child: Row(
            children: [
              if (leading != null) ...[leading!, const SizedBox(width: Space.md)],
              Expanded(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      maxLines: titleMaxLines,
                      overflow: TextOverflow.ellipsis,
                      style: titleStyle ?? t.bodyLarge?.copyWith(fontWeight: FontWeight.w500, height: 1.4),
                    ),
                    if (subtitle != null)
                      DefaultTextStyle.merge(
                        style: t.bodySmall,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        child: subtitle!,
                      ),
                  ],
                ),
              ),
              if (trailing != null) ...[const SizedBox(width: Space.sm), trailing!],
              if (showChevron) ...[
                const SizedBox(width: Space.xs),
                Icon(Icons.chevron_right_rounded, size: 20, color: scheme.onSurfaceVariant.withValues(alpha: 0.7)),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

/// Row with a trailing value and chevron (settings / pickers).
class LkValueRow extends StatelessWidget {
  const LkValueRow({
    super.key,
    required this.title,
    this.value,
    this.leading,
    this.onTap,
    this.subtitle,
    this.valueColor,
  });
  final String title;
  final String? value;
  final String? subtitle;
  final Widget? leading;
  final VoidCallback? onTap;
  final Color? valueColor;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    final scheme = Theme.of(context).colorScheme;
    return LkRow(
      title: title,
      leading: leading,
      onTap: onTap,
      subtitle: subtitle == null ? null : Text(subtitle!),
      showChevron: onTap != null,
      trailing: value == null
          ? null
          : ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 160),
              child: Text(
                value!,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                textAlign: TextAlign.end,
                style: t.bodyMedium?.copyWith(color: valueColor ?? scheme.onSurfaceVariant),
              ),
            ),
    );
  }
}

/// Row with a switch; the whole row toggles.
class LkSwitchRow extends StatelessWidget {
  const LkSwitchRow({
    super.key,
    required this.title,
    required this.value,
    required this.onChanged,
    this.subtitle,
    this.leading,
  });
  final String title;
  final String? subtitle;
  final bool value;
  final ValueChanged<bool>? onChanged;
  final Widget? leading;

  @override
  Widget build(BuildContext context) {
    return LkRow(
      title: title,
      leading: leading,
      subtitle: subtitle == null ? null : Text(subtitle!),
      onTap: onChanged == null ? null : () => onChanged!(!value),
      trailing: Switch(value: value, onChanged: onChanged),
    );
  }
}
