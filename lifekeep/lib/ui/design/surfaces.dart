import 'package:flutter/material.dart';

import '../../core/theme/tokens.dart';

/// Centers content and caps its width on tablets / landscape.
class ContentWidth extends StatelessWidget {
  const ContentWidth({super.key, required this.child});
  final Widget child;

  @override
  Widget build(BuildContext context) => Align(
    alignment: Alignment.topCenter,
    child: ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: Space.maxContentWidth),
      child: child,
    ),
  );
}

/// Small section label with optional trailing action ("See all").
class LkSectionHeader extends StatelessWidget {
  const LkSectionHeader({super.key, required this.title, this.actionLabel, this.onAction, this.count, this.padding});

  final String title;
  final String? actionLabel;
  final VoidCallback? onAction;
  final int? count;
  final EdgeInsets? padding;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    final scheme = Theme.of(context).colorScheme;
    return Padding(
      padding: padding ?? const EdgeInsets.fromLTRB(Space.xs, Space.xl, Space.xs, Space.sm),
      child: Row(
        children: [
          // Expanded (not Flexible + Spacer, which would split the row 50/50)
          // so long titles get all the room the action does not need.
          Expanded(
            child: Row(
              children: [
                Flexible(
                  child: Text(title, style: t.titleSmall, maxLines: 1, overflow: TextOverflow.ellipsis),
                ),
                if (count != null && count! > 0) ...[
                  const SizedBox(width: Space.sm),
                  Text('$count', style: t.labelMedium?.copyWith(color: scheme.onSurfaceVariant)),
                ],
              ],
            ),
          ),
          if (actionLabel != null && onAction != null)
            InkWell(
              borderRadius: Radii.brSm,
              onTap: onAction,
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: Space.xs, vertical: Space.xxs),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(actionLabel!, style: t.labelMedium?.copyWith(color: scheme.primary)),
                    Icon(Icons.chevron_right_rounded, size: 18, color: scheme.primary),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }
}

/// A rounded surface that groups related rows, separated by inset dividers.
/// Used instead of one card per item, which keeps lists calm and dense.
class LkGroup extends StatelessWidget {
  const LkGroup({
    super.key,
    required this.children,
    this.dividerIndent = Space.lg,
    this.color,
    this.padding = EdgeInsets.zero,
  });

  final List<Widget> children;
  final double dividerIndent;
  final Color? color;
  final EdgeInsets padding;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Material(
      color: color ?? scheme.surface,
      borderRadius: Radii.brLg,
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: padding,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            for (var i = 0; i < children.length; i++) ...[
              if (i > 0) Divider(height: 1, indent: dividerIndent, endIndent: 0),
              children[i],
            ],
          ],
        ),
      ),
    );
  }
}

/// Plain padded surface (for summaries, banners, form blocks).
class LkPanel extends StatelessWidget {
  const LkPanel({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(Space.lg),
    this.color,
    this.onTap,
    this.side,
  });
  final Widget child;
  final EdgeInsets padding;
  final Color? color;
  final VoidCallback? onTap;
  final BorderSide? side;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Material(
      color: color ?? scheme.surface,
      shape: RoundedRectangleBorder(borderRadius: Radii.brLg, side: side ?? BorderSide.none),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(padding: padding, child: child),
      ),
    );
  }
}

/// Coloured informational banner (info / warning / danger / success).
class LkBanner extends StatelessWidget {
  const LkBanner({
    super.key,
    required this.icon,
    required this.message,
    required this.color,
    required this.background,
    this.title,
    this.action,
  });
  final IconData icon;
  final String? title;
  final String message;
  final Color color;
  final Color background;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    return Container(
      padding: const EdgeInsets.fromLTRB(Space.md, Space.md, Space.md, Space.md),
      decoration: BoxDecoration(color: background, borderRadius: Radii.brMd),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.only(top: 1),
            child: Icon(icon, size: 20, color: color),
          ),
          const SizedBox(width: Space.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (title != null) Text(title!, style: t.labelLarge?.copyWith(color: color)),
                Text(
                  message,
                  style: t.bodySmall?.copyWith(color: Theme.of(context).colorScheme.onSurface.withValues(alpha: 0.8)),
                ),
              ],
            ),
          ),
          if (action != null) ...[const SizedBox(width: Space.sm), action!],
        ],
      ),
    );
  }
}
