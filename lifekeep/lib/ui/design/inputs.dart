import 'package:flutter/material.dart';

import 'fancy.dart';

import '../../core/l10n/lang.dart';
import '../../core/theme/tokens.dart';
import 'badges.dart';
import 'motion.dart';

/// Rounded search field with a clear button.
class SearchField extends StatelessWidget {
  const SearchField({
    super.key,
    required this.controller,
    required this.hint,
    required this.onChanged,
    this.autofocus = false,
    this.onSubmitted,
  });
  final TextEditingController controller;
  final String hint;
  final ValueChanged<String> onChanged;
  final ValueChanged<String>? onSubmitted;
  final bool autofocus;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return TextField(
      controller: controller,
      autofocus: autofocus,
      onChanged: onChanged,
      onSubmitted: onSubmitted,
      textInputAction: TextInputAction.search,
      style: Theme.of(context).textTheme.bodyMedium,
      decoration: InputDecoration(
        hintText: hint,
        fillColor: scheme.surface,
        contentPadding: const EdgeInsets.symmetric(vertical: 11),
        prefixIcon: const Icon(Icons.search_rounded, size: 22),
        suffixIcon: ValueListenableBuilder(
          valueListenable: controller,
          builder: (context, v, _) => v.text.isEmpty
              ? const SizedBox.shrink()
              : IconButton(
                  tooltip: context.tr('ล้าง', 'Clear'),
                  icon: const Icon(Icons.cancel_rounded, size: 20),
                  onPressed: () {
                    controller.clear();
                    onChanged('');
                  },
                ),
        ),
      ),
    );
  }
}

/// Field-like tappable row for pickers (date, time, category…).
class PickerField extends StatelessWidget {
  const PickerField({
    super.key,
    required this.icon,
    required this.label,
    required this.value,
    required this.onTap,
    this.onClear,
    this.leading,
  });
  final IconData icon;
  final String label;
  final String? value;
  final VoidCallback onTap;
  final VoidCallback? onClear;

  /// Optional custom leading widget (e.g. category badge) replacing [icon].
  final Widget? leading;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    return Material(
      color: scheme.surfaceContainerHigh,
      borderRadius: Radii.brMd,
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(
            Space.md,
            Space.sm,
            Space.xs,
            Space.sm,
          ),
          child: Row(
            children: [
              leading ?? Icon(icon, size: 20, color: scheme.onSurfaceVariant),
              const SizedBox(width: Space.md),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(label, style: t.labelSmall),
                    Text(
                      value ?? context.tr('ไม่ระบุ', 'Not set'),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: t.bodyMedium?.copyWith(
                        fontWeight: value == null
                            ? FontWeight.w400
                            : FontWeight.w500,
                        color: value == null
                            ? scheme.onSurfaceVariant
                            : scheme.onSurface,
                      ),
                    ),
                  ],
                ),
              ),
              if (value != null && onClear != null)
                IconButton(
                  tooltip: context.tr('ล้าง', 'Clear'),
                  icon: const Icon(Icons.close_rounded, size: 18),
                  onPressed: onClear,
                )
              else
                Padding(
                  padding: const EdgeInsets.all(Space.sm),
                  child: Icon(
                    Icons.expand_more_rounded,
                    size: 20,
                    color: scheme.onSurfaceVariant,
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Titled block of form controls on a surface.
class FormBlock extends StatelessWidget {
  const FormBlock({
    super.key,
    required this.title,
    required this.children,
    this.icon,
    this.trailing,
  });
  final String title;
  final IconData? icon;
  final List<Widget> children;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.only(bottom: Space.md),
      child: Material(
        color: scheme.surface,
        borderRadius: Radii.brLg,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(
            Space.lg,
            Space.md,
            Space.lg,
            Space.lg,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                children: [
                  if (icon != null) ...[
                    Icon(icon, size: 18, color: scheme.primary),
                    const SizedBox(width: Space.sm),
                  ],
                  Expanded(child: Text(title, style: t.titleSmall)),
                  ?trailing,
                ],
              ),
              const SizedBox(height: Space.md),
              for (var i = 0; i < children.length; i++) ...[
                if (i > 0) const SizedBox(height: Space.md),
                children[i],
              ],
            ],
          ),
        ),
      ),
    );
  }
}

/// Compact tonal action button with icon (quick add).
class QuickActionButton extends StatelessWidget {
  const QuickActionButton({
    super.key,
    required this.icon,
    required this.label,
    required this.onTap,
    this.color,
  });
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  /// Icon colour (defaults to the brand blue).
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final c = color ?? scheme.primary;
    return Pressable(
      child: DecoratedBox(
        decoration: BoxDecoration(
          borderRadius: Radii.brLg,
          boxShadow: lkShadow(context),
        ),
        child: Material(
          color: scheme.surface,
          borderRadius: Radii.brLg,
          clipBehavior: Clip.antiAlias,
          child: InkWell(
            onTap: onTap,
            child: SizedBox(
              height: 104,
              child: Stack(
                children: [
                  Positioned(
                    right: 6,
                    top: 0,
                    bottom: 18,
                    child: Icon(
                      Icons.chevron_right_rounded,
                      size: 22,
                      color: scheme.onSurfaceVariant.withValues(alpha: 0.6),
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.fromLTRB(
                      Space.md,
                      Space.lg,
                      Space.md,
                      Space.md,
                    ),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Icon(icon, size: 36, color: c),
                        Text(
                          label,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: Theme.of(context).textTheme.titleSmall
                              ?.copyWith(
                                color: scheme.onSurface,
                                fontWeight: FontWeight.w700,
                              ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Equal-width segmented tab bar (with counters) bound to a [TabController].
/// Labels scale down instead of overflowing on narrow phones.
class SegmentTabs extends StatelessWidget implements PreferredSizeWidget {
  const SegmentTabs({
    super.key,
    required this.controller,
    required this.labels,
    this.counts,
    this.alertIndex,
    this.icons,
  });
  final TabController controller;
  final List<String> labels;
  final List<int>? counts;
  final int? alertIndex;
  final List<IconData>? icons;

  @override
  Size get preferredSize => const Size.fromHeight(64);

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    return SizedBox(
      height: 64,
      child: AnimatedBuilder(
        animation: controller,
        builder: (context, _) => Padding(
          padding: const EdgeInsets.fromLTRB(
            Space.page,
            Space.xs,
            Space.page,
            Space.sm,
          ),
          child: Row(
            children: [
              for (var i = 0; i < labels.length; i++) ...[
                if (i > 0) const SizedBox(width: 6),
                Expanded(child: _pill(context, i, scheme, t)),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _pill(BuildContext context, int i, ColorScheme scheme, TextTheme t) {
    final on = controller.index == i;
    final fg = on ? Colors.white : scheme.onSurface;
    return DecoratedBox(
      decoration: BoxDecoration(
        gradient: on ? lkGradient : null,
        color: on ? null : scheme.surface,
        borderRadius: BorderRadius.circular(Radii.pill),
        boxShadow: on
            ? [
                BoxShadow(
                  color: lkBlue.withValues(alpha: 0.3),
                  blurRadius: 12,
                  offset: const Offset(0, 4),
                ),
              ]
            : lkShadow(context),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(Radii.pill),
          onTap: () => controller.animateTo(i),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: Space.sm),
            child: Center(
              child: FittedBox(
                fit: BoxFit.scaleDown,
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (icons != null) ...[
                      Icon(
                        icons![i],
                        size: 20,
                        color: on ? Colors.white : scheme.onSurfaceVariant,
                      ),
                      const SizedBox(width: 6),
                    ],
                    Text(labels[i], style: t.labelLarge?.copyWith(color: fg)),
                    if (counts != null && counts![i] > 0) ...[
                      const SizedBox(width: 6),
                      CountBadge(
                        count: counts![i],
                        alert: i == alertIndex && !on,
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
