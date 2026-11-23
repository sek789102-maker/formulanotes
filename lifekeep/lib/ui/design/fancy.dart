import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';
import '../../core/theme/tokens.dart';

/// Bright blue look: gradient tiles, soft blue shadows, wavy backdrop, floating nav.

const lkBlue = Color(0xFF1F5EFF);
const lkBlueDeep = Color(0xFF1240C8);
const lkBlueLight = Color(0xFF4A8BFF);

const lkGradient = LinearGradient(
  begin: Alignment.topLeft,
  end: Alignment.bottomRight,
  colors: [lkBlueLight, lkBlue, lkBlueDeep],
);

bool _dark(BuildContext context) =>
    Theme.of(context).brightness == Brightness.dark;

/// Soft blue-tinted shadow under cards.
List<BoxShadow> lkShadow(BuildContext context, {double strength = 1}) =>
    _dark(context)
    ? const []
    : [
        BoxShadow(
          color: const Color(0xFF2A4FB8).withValues(alpha: 0.08 * strength),
          blurRadius: 24,
          offset: const Offset(0, 8),
        ),
      ];

/// Page background: pale blue with soft waves at the top-right and bottom.
class LkBackdrop extends StatelessWidget {
  const LkBackdrop({super.key, required this.child});
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final dark = _dark(context);
    final bg = Theme.of(context).scaffoldBackgroundColor;
    return DecoratedBox(
      decoration: BoxDecoration(color: bg),
      child: Stack(
        children: [
          Positioned.fill(
            child: CustomPaint(painter: _WavePainter(dark: dark)),
          ),
          child,
        ],
      ),
    );
  }
}

class _WavePainter extends CustomPainter {
  _WavePainter({required this.dark});
  final bool dark;

  @override
  void paint(Canvas canvas, Size size) {
    final w = size.width;
    final h = size.height;
    final a = dark ? 0.10 : 1.0;
    // Glow at the top-right corner.
    canvas.drawCircle(
      Offset(w * 0.95, -h * 0.02),
      w * 0.75,
      Paint()
        ..shader =
            RadialGradient(
              colors: [
                const Color(0xFFBFD3FF).withValues(alpha: 0.75 * a),
                const Color(0x00BFD3FF),
              ],
            ).createShader(
              Rect.fromCircle(
                center: Offset(w * 0.95, -h * 0.02),
                radius: w * 0.75,
              ),
            ),
    );
    // Two soft waves near the bottom.
    Path wave(double top, double amp) => Path()
      ..moveTo(0, h * top)
      ..cubicTo(
        w * 0.30,
        h * (top - amp),
        w * 0.60,
        h * (top + amp),
        w,
        h * (top - amp * 0.6),
      )
      ..lineTo(w, h)
      ..lineTo(0, h)
      ..close();
    canvas.drawPath(
      wave(0.80, 0.05),
      Paint()..color = const Color(0xFFDCE6FF).withValues(alpha: 0.65 * a),
    );
    canvas.drawPath(
      wave(0.86, 0.04),
      Paint()..color = const Color(0xFFCCDAFF).withValues(alpha: 0.55 * a),
    );
  }

  @override
  bool shouldRepaint(_WavePainter old) => old.dark != dark;
}

/// Blue gradient rounded square with a white icon (screen headers, welcome card).
class GradientTile extends StatelessWidget {
  const GradientTile({
    super.key,
    required this.icon,
    this.size = 58,
    this.colors,
  });
  final IconData icon;
  final double size;
  final List<Color>? colors;

  @override
  Widget build(BuildContext context) => Container(
    width: size,
    height: size,
    decoration: BoxDecoration(
      gradient: LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: colors ?? const [lkBlueLight, lkBlue, lkBlueDeep],
      ),
      borderRadius: BorderRadius.circular(size * 0.3),
      boxShadow: [
        BoxShadow(
          color: (colors?.last ?? lkBlue).withValues(alpha: 0.35),
          blurRadius: 16,
          offset: const Offset(0, 6),
        ),
      ],
    ),
    child: Icon(icon, color: Colors.white, size: size * 0.5),
  );
}

/// Title block used as the AppBar title of the main tabs.
class TabTitle extends StatelessWidget {
  const TabTitle({
    super.key,
    required this.icon,
    required this.title,
    required this.subtitle,
  });
  final IconData icon;
  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    return Row(
      children: [
        GradientTile(icon: icon, size: 56),
        const SizedBox(width: Space.lg),
        Expanded(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: t.headlineSmall?.copyWith(fontSize: 26),
              ),
              Text(
                subtitle,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: t.bodySmall,
              ),
            ],
          ),
        ),
      ],
    );
  }
}

/// White rounded-square style for header icon buttons.
ButtonStyle lkHeaderButtonStyle(BuildContext context) {
  final scheme = Theme.of(context).colorScheme;
  return IconButton.styleFrom(
    backgroundColor: scheme.surface,
    foregroundColor: scheme.onSurface,
    minimumSize: const Size(50, 50),
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
    elevation: _dark(context) ? 0 : 2,
    shadowColor: const Color(0xFF2A4FB8).withValues(alpha: 0.25),
  );
}

/// Pill button with the blue gradient.
class GradientButton extends StatelessWidget {
  const GradientButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.icon = Icons.add_rounded,
    this.trailing,
    this.height = 58,
  });
  final String label;
  final VoidCallback onPressed;
  final IconData? icon;
  final IconData? trailing;
  final double height;

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    return DecoratedBox(
      decoration: BoxDecoration(
        gradient: lkGradient,
        borderRadius: BorderRadius.circular(height),
        boxShadow: [
          BoxShadow(
            color: lkBlue.withValues(alpha: 0.35),
            blurRadius: 18,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(height),
          onTap: onPressed,
          child: SizedBox(
            height: height,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: Space.xxl),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  if (icon != null) ...[
                    Icon(icon, color: Colors.white, size: 26),
                    const SizedBox(width: Space.md),
                  ],
                  Flexible(
                    child: Text(
                      label,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: t.titleMedium?.copyWith(
                        color: Colors.white,
                        fontWeight: FontWeight.w700,
                        fontSize: 18,
                      ),
                    ),
                  ),
                  if (trailing != null) ...[
                    const SizedBox(width: Space.md),
                    Icon(trailing, color: Colors.white),
                  ],
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Round gradient "+" button with a blue glow.
class GradientFab extends StatelessWidget {
  const GradientFab({
    super.key,
    required this.onPressed,
    required this.tooltip,
    this.heroTag,
  });
  final VoidCallback onPressed;
  final String tooltip;
  final Object? heroTag;

  @override
  Widget build(BuildContext context) => Tooltip(
    message: tooltip,
    child: Container(
      width: 64,
      height: 64,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: lkGradient,
        boxShadow: [
          BoxShadow(
            color: lkBlue.withValues(alpha: 0.45),
            blurRadius: 22,
            offset: const Offset(0, 8),
          ),
          BoxShadow(
            color: lkBlueLight.withValues(alpha: 0.25),
            blurRadius: 2,
            spreadRadius: 6,
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        shape: const CircleBorder(),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onPressed,
          child: const Icon(Icons.add_rounded, color: Colors.white, size: 34),
        ),
      ),
    ),
  );
}

/// Floating rounded bottom bar: the selected tab gets a soft pill and an underline.
class FloatingNavBar extends StatelessWidget {
  const FloatingNavBar({
    super.key,
    required this.items,
    required this.selected,
    required this.onSelect,
  });
  final List<(IconData, IconData, String)> items;
  final int selected;
  final ValueChanged<int> onSelect;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    return SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(Space.md, 0, Space.md, Space.sm),
        child: Container(
          height: 76,
          decoration: BoxDecoration(
            color: scheme.surface,
            borderRadius: BorderRadius.circular(30),
            boxShadow: lkShadow(context, strength: 1.6),
            border: _dark(context)
                ? Border.all(color: scheme.outlineVariant)
                : null,
          ),
          child: Row(
            children: [
              for (var i = 0; i < items.length; i++)
                Expanded(
                  child: InkWell(
                    borderRadius: BorderRadius.circular(24),
                    onTap: () => onSelect(i),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        AnimatedContainer(
                          duration: Motion.medium,
                          curve: Motion.curve,
                          width: 64,
                          height: 34,
                          decoration: BoxDecoration(
                            color: i == selected
                                ? scheme.primaryContainer
                                : Colors.transparent,
                            borderRadius: BorderRadius.circular(17),
                          ),
                          child: Icon(
                            i == selected ? items[i].$2 : items[i].$1,
                            size: 26,
                            color: i == selected
                                ? scheme.primary
                                : scheme.onSurfaceVariant,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          items[i].$3,
                          maxLines: 1,
                          style: t.labelMedium?.copyWith(
                            fontSize: 13,
                            color: i == selected
                                ? scheme.primary
                                : scheme.onSurfaceVariant,
                            fontWeight: i == selected
                                ? FontWeight.w700
                                : FontWeight.w500,
                          ),
                        ),
                        const SizedBox(height: 3),
                        AnimatedContainer(
                          duration: Motion.medium,
                          width: i == selected ? 30 : 0,
                          height: 3,
                          decoration: BoxDecoration(
                            color: scheme.primary,
                            borderRadius: BorderRadius.circular(2),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Illustration that fades into the page at its edges.
class SoftIllustration extends StatelessWidget {
  const SoftIllustration({super.key, required this.asset, this.height = 180});
  final String asset;
  final double height;

  @override
  Widget build(BuildContext context) => SizedBox(
    height: height,
    child: ShaderMask(
      blendMode: BlendMode.dstIn,
      shaderCallback: (r) => const RadialGradient(
        radius: 0.72,
        colors: [Colors.black, Colors.black, Colors.transparent],
        stops: [0, 0.72, 1],
      ).createShader(r),
      child: Opacity(
        opacity: Theme.of(context).brightness == Brightness.dark ? 0.85 : 1,
        child: Image.asset(asset, fit: BoxFit.contain),
      ),
    ),
  );
}

/// Keeps the [AppTheme] import used (for consumers importing this file alone).
const lkFontFamily = AppTheme.fontFamily;
