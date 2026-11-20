import 'package:flutter/material.dart';

/// LIFEKEEP brand palette. Calm, low-saturation colours.
class AppColors {
  AppColors._();

  static const background = Color(0xFFF5F6F8);
  static const primary = Color(0xFF233B67);
  static const secondary = Color(0xFF64748B);
  static const surface = Color(0xFFFFFFFF);

  static const success = Color(0xFF2E7D5B);
  static const warning = Color(0xFFC77A2A);
  static const danger = Color(0xFFB4473F);

  // Dark mode counterparts
  static const darkBackground = Color(0xFF0F141B);
  static const darkSurface = Color(0xFF171D26);
  static const darkSurfaceHigh = Color(0xFF1F2733);
  static const darkPrimary = Color(0xFF9DB4E0);

  static const successDark = Color(0xFF6CC59A);
  static const warningDark = Color(0xFFE8A866);
  static const dangerDark = Color(0xFFE88A82);

  /// Palette offered for categories (muted tones).
  static const categoryPalette = <Color>[
    Color(0xFF233B67),
    Color(0xFF3B6EA8),
    Color(0xFF2E7D7A),
    Color(0xFF2E7D5B),
    Color(0xFF7A8B2E),
    Color(0xFFC77A2A),
    Color(0xFFB4473F),
    Color(0xFF9C4F7E),
    Color(0xFF6B5BA8),
    Color(0xFF64748B),
  ];
}

/// Semantic status colours that adapt to light/dark.
@immutable
class StatusColors extends ThemeExtension<StatusColors> {
  const StatusColors({
    required this.success,
    required this.warning,
    required this.danger,
    required this.successContainer,
    required this.warningContainer,
    required this.dangerContainer,
  });

  final Color success;
  final Color warning;
  final Color danger;
  final Color successContainer;
  final Color warningContainer;
  final Color dangerContainer;

  static const light = StatusColors(
    success: AppColors.success,
    warning: AppColors.warning,
    danger: AppColors.danger,
    successContainer: Color(0xFFE3F1EA),
    warningContainer: Color(0xFFFBEEDF),
    dangerContainer: Color(0xFFF7E3E1),
  );

  static const dark = StatusColors(
    success: AppColors.successDark,
    warning: AppColors.warningDark,
    danger: AppColors.dangerDark,
    successContainer: Color(0xFF1C3328),
    warningContainer: Color(0xFF3A2A18),
    dangerContainer: Color(0xFF3B1F1C),
  );

  @override
  StatusColors copyWith({
    Color? success,
    Color? warning,
    Color? danger,
    Color? successContainer,
    Color? warningContainer,
    Color? dangerContainer,
  }) {
    return StatusColors(
      success: success ?? this.success,
      warning: warning ?? this.warning,
      danger: danger ?? this.danger,
      successContainer: successContainer ?? this.successContainer,
      warningContainer: warningContainer ?? this.warningContainer,
      dangerContainer: dangerContainer ?? this.dangerContainer,
    );
  }

  @override
  StatusColors lerp(ThemeExtension<StatusColors>? other, double t) {
    if (other is! StatusColors) return this;
    return StatusColors(
      success: Color.lerp(success, other.success, t)!,
      warning: Color.lerp(warning, other.warning, t)!,
      danger: Color.lerp(danger, other.danger, t)!,
      successContainer: Color.lerp(successContainer, other.successContainer, t)!,
      warningContainer: Color.lerp(warningContainer, other.warningContainer, t)!,
      dangerContainer: Color.lerp(dangerContainer, other.dangerContainer, t)!,
    );
  }
}

extension StatusColorsX on BuildContext {
  StatusColors get status =>
      Theme.of(this).extension<StatusColors>() ?? StatusColors.light;
}

class AppTheme {
  AppTheme._();

  static const radius = 18.0;

  static ThemeData light() {
    final base = ColorScheme.fromSeed(
      seedColor: AppColors.primary,
      brightness: Brightness.light,
    );
    final scheme = base.copyWith(
      primary: AppColors.primary,
      onPrimary: Colors.white,
      primaryContainer: const Color(0xFFDCE4F3),
      onPrimaryContainer: const Color(0xFF14254A),
      secondary: AppColors.secondary,
      onSecondary: Colors.white,
      secondaryContainer: const Color(0xFFE6EAF0),
      onSecondaryContainer: const Color(0xFF2B3442),
      surface: AppColors.surface,
      onSurface: const Color(0xFF1B2230),
      onSurfaceVariant: const Color(0xFF5B6576),
      surfaceContainerLowest: AppColors.surface,
      surfaceContainerLow: const Color(0xFFF9FAFB),
      surfaceContainer: AppColors.background,
      surfaceContainerHigh: const Color(0xFFEDEFF3),
      surfaceContainerHighest: const Color(0xFFE5E8ED),
      outline: const Color(0xFFC5CBD5),
      outlineVariant: const Color(0xFFE3E6EB),
      error: AppColors.danger,
    );
    return _build(scheme, AppColors.background, StatusColors.light);
  }

  static ThemeData dark() {
    final base = ColorScheme.fromSeed(
      seedColor: AppColors.primary,
      brightness: Brightness.dark,
    );
    final scheme = base.copyWith(
      primary: AppColors.darkPrimary,
      onPrimary: const Color(0xFF10203F),
      primaryContainer: const Color(0xFF2A3F66),
      onPrimaryContainer: const Color(0xFFDCE4F3),
      secondary: const Color(0xFFA3AFC0),
      onSecondary: const Color(0xFF1B2230),
      secondaryContainer: const Color(0xFF2A3240),
      onSecondaryContainer: const Color(0xFFDDE3EB),
      surface: AppColors.darkSurface,
      onSurface: const Color(0xFFE6E9EF),
      onSurfaceVariant: const Color(0xFFA3ACBA),
      surfaceContainerLowest: AppColors.darkBackground,
      surfaceContainerLow: const Color(0xFF141A22),
      surfaceContainer: AppColors.darkBackground,
      surfaceContainerHigh: AppColors.darkSurfaceHigh,
      surfaceContainerHighest: const Color(0xFF283141),
      outline: const Color(0xFF465062),
      outlineVariant: const Color(0xFF2B3442),
      error: AppColors.dangerDark,
    );
    return _build(scheme, AppColors.darkBackground, StatusColors.dark);
  }

  static ThemeData _build(ColorScheme scheme, Color background, StatusColors status) {
    final textTheme = Typography.material2021(platform: TargetPlatform.android)
        .englishLike
        .merge(scheme.brightness == Brightness.dark
            ? Typography.material2021().white
            : Typography.material2021().black)
        .apply(bodyColor: scheme.onSurface, displayColor: scheme.onSurface);

    final roundedShape = RoundedRectangleBorder(borderRadius: BorderRadius.circular(radius));

    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: background,
      extensions: [status],
      textTheme: textTheme.copyWith(
        headlineSmall: textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w700, letterSpacing: -0.2),
        titleLarge: textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
        titleMedium: textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
        titleSmall: textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w600),
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: background,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0.5,
        centerTitle: false,
        foregroundColor: scheme.onSurface,
        titleTextStyle: textTheme.titleLarge?.copyWith(
          fontWeight: FontWeight.w700,
          color: scheme.onSurface,
        ),
      ),
      cardTheme: CardThemeData(
        color: scheme.surface,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(radius),
          side: BorderSide(color: scheme.outlineVariant),
        ),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: scheme.surface,
        surfaceTintColor: Colors.transparent,
        indicatorColor: scheme.primaryContainer,
        elevation: 0,
        height: 68,
        labelTextStyle: WidgetStateProperty.resolveWith((states) {
          final selected = states.contains(WidgetState.selected);
          return TextStyle(
            fontSize: 12,
            fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
            color: selected ? scheme.primary : scheme.onSurfaceVariant,
          );
        }),
        iconTheme: WidgetStateProperty.resolveWith((states) {
          final selected = states.contains(WidgetState.selected);
          return IconThemeData(
            color: selected ? scheme.onPrimaryContainer : scheme.onSurfaceVariant,
          );
        }),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: scheme.surface,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: BorderSide(color: scheme.outlineVariant),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: BorderSide(color: scheme.outlineVariant),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: BorderSide(color: scheme.primary, width: 1.5),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: BorderSide(color: scheme.error),
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size(0, 48),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
          textStyle: textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w600, fontSize: 15),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size(0, 48),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
          side: BorderSide(color: scheme.outline),
          textStyle: textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w600, fontSize: 15),
        ),
      ),
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: scheme.primary,
        foregroundColor: scheme.onPrimary,
        elevation: 2,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      ),
      chipTheme: ChipThemeData(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        side: BorderSide(color: scheme.outlineVariant),
        backgroundColor: scheme.surface,
        selectedColor: scheme.primaryContainer,
        labelStyle: textTheme.labelLarge?.copyWith(color: scheme.onSurface, fontWeight: FontWeight.w500),
      ),
      listTileTheme: ListTileThemeData(
        shape: roundedShape,
        iconColor: scheme.onSurfaceVariant,
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: scheme.surface,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      ),
      bottomSheetTheme: BottomSheetThemeData(
        backgroundColor: scheme.surface,
        surfaceTintColor: Colors.transparent,
        showDragHandle: true,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
      dividerTheme: DividerThemeData(color: scheme.outlineVariant, space: 1),
      pageTransitionsTheme: const PageTransitionsTheme(
        builders: {
          TargetPlatform.android: FadeForwardsPageTransitionsBuilder(),
        },
      ),
    );
  }
}
