import 'package:flutter/material.dart';

import 'tokens.dart';

/// LIFEKEEP brand palette: calm neutrals with a trustworthy navy.
class AppColors {
  AppColors._();

  static const background = Color(0xFFF0F4FE);
  static const primary = Color(0xFF1F5EFF);
  static const secondary = Color(0xFF5B6785);
  static const surface = Color(0xFFFFFFFF);

  static const success = Color(0xFF2E7D5B);
  static const warning = Color(0xFFC0742A);
  static const danger = Color(0xFFB4473F);

  static const darkBackground = Color(0xFF0B1020);
  static const darkSurface = Color(0xFF141B2E);
  static const darkPrimary = Color(0xFF8DB0FF);

  /// Muted palette offered for categories and events.
  static const categoryPalette = <Color>[
    Color(0xFF233B67),
    Color(0xFF3B6EA8),
    Color(0xFF2E7D7A),
    Color(0xFF2E7D5B),
    Color(0xFF7A8B2E),
    Color(0xFFC0742A),
    Color(0xFFB4473F),
    Color(0xFF9C4F7E),
    Color(0xFF6B5BA8),
    Color(0xFF64748B),
  ];
}

/// Semantic status colours (success / warning / danger) for both themes.
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
    successContainer: Color(0xFFE4F2EA),
    warningContainer: Color(0xFFFBEFE2),
    dangerContainer: Color(0xFFF8E5E3),
  );

  static const dark = StatusColors(
    success: Color(0xFF72C79F),
    warning: Color(0xFFE6A86A),
    danger: Color(0xFFEC8D85),
    successContainer: Color(0xFF1A3226),
    warningContainer: Color(0xFF3A2A18),
    dangerContainer: Color(0xFF3C201D),
  );

  @override
  StatusColors copyWith({
    Color? success,
    Color? warning,
    Color? danger,
    Color? successContainer,
    Color? warningContainer,
    Color? dangerContainer,
  }) => StatusColors(
    success: success ?? this.success,
    warning: warning ?? this.warning,
    danger: danger ?? this.danger,
    successContainer: successContainer ?? this.successContainer,
    warningContainer: warningContainer ?? this.warningContainer,
    dangerContainer: dangerContainer ?? this.dangerContainer,
  );

  @override
  StatusColors lerp(ThemeExtension<StatusColors>? other, double t) {
    if (other is! StatusColors) return this;
    return StatusColors(
      success: Color.lerp(success, other.success, t)!,
      warning: Color.lerp(warning, other.warning, t)!,
      danger: Color.lerp(danger, other.danger, t)!,
      successContainer: Color.lerp(
        successContainer,
        other.successContainer,
        t,
      )!,
      warningContainer: Color.lerp(
        warningContainer,
        other.warningContainer,
        t,
      )!,
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

  static const fontFamily = 'IBMPlexSansThai';

  /// Kept for older call sites; prefer [Radii].
  static const radius = Radii.lg;

  static ThemeData light() {
    final scheme = ColorScheme.fromSeed(seedColor: AppColors.primary).copyWith(
      primary: AppColors.primary,
      onPrimary: Colors.white,
      primaryContainer: const Color(0xFFE1EAFF),
      onPrimaryContainer: const Color(0xFF0B2A8A),
      secondary: AppColors.secondary,
      onSecondary: Colors.white,
      secondaryContainer: const Color(0xFFE9EDF2),
      onSecondaryContainer: const Color(0xFF283241),
      tertiary: const Color(0xFF2E7D7A),
      surface: AppColors.surface,
      onSurface: const Color(0xFF0D1B45),
      onSurfaceVariant: const Color(0xFF5B6785),
      surfaceContainerLowest: AppColors.surface,
      surfaceContainerLow: const Color(0xFFF8FAFF),
      surfaceContainer: AppColors.background,
      surfaceContainerHigh: const Color(0xFFEEF3FF),
      surfaceContainerHighest: const Color(0xFFE4EBFB),
      outline: const Color(0xFFC6D1EA),
      outlineVariant: const Color(0xFFE3E9F7),
      error: AppColors.danger,
      shadow: const Color(0xFF0B1A33),
    );
    return _build(scheme, AppColors.background, StatusColors.light);
  }

  static ThemeData dark() {
    final scheme =
        ColorScheme.fromSeed(
          seedColor: AppColors.primary,
          brightness: Brightness.dark,
        ).copyWith(
          primary: AppColors.darkPrimary,
          onPrimary: const Color(0xFF0F2042),
          primaryContainer: const Color(0xFF1C3570),
          onPrimaryContainer: const Color(0xFFDCE6FF),
          secondary: const Color(0xFFA6B1C2),
          onSecondary: const Color(0xFF1A2230),
          secondaryContainer: const Color(0xFF263040),
          onSecondaryContainer: const Color(0xFFDDE3EB),
          tertiary: const Color(0xFF7CC7C2),
          surface: AppColors.darkSurface,
          onSurface: const Color(0xFFE7EAF0),
          onSurfaceVariant: const Color(0xFFA2ACBB),
          surfaceContainerLowest: AppColors.darkBackground,
          surfaceContainerLow: const Color(0xFF131921),
          surfaceContainer: AppColors.darkBackground,
          surfaceContainerHigh: const Color(0xFF1E2632),
          surfaceContainerHighest: const Color(0xFF253041),
          outline: const Color(0xFF465163),
          outlineVariant: const Color(0xFF242D3A),
          error: const Color(0xFFEC8D85),
          shadow: Colors.black,
        );
    return _build(scheme, AppColors.darkBackground, StatusColors.dark);
  }

  /// Type scale tuned for Thai: generous line height so stacked tone marks
  /// and vowels never clip, and a clear weight hierarchy.
  static TextTheme _text(Color onSurface, Color muted) {
    TextStyle s(
      double size,
      FontWeight w,
      double h, {
      Color? c,
      double ls = 0,
    }) => TextStyle(
      fontFamily: fontFamily,
      fontSize: size,
      fontWeight: w,
      height: h,
      color: c ?? onSurface,
      letterSpacing: ls,
    );
    return TextTheme(
      displaySmall: s(30, FontWeight.w700, 1.25, ls: -0.4),
      headlineMedium: s(26, FontWeight.w700, 1.3, ls: -0.3),
      headlineSmall: s(22, FontWeight.w700, 1.3, ls: -0.2),
      titleLarge: s(19, FontWeight.w700, 1.35),
      titleMedium: s(16, FontWeight.w600, 1.4),
      titleSmall: s(15, FontWeight.w600, 1.4),
      bodyLarge: s(15, FontWeight.w400, 1.55),
      bodyMedium: s(14, FontWeight.w400, 1.5),
      bodySmall: s(12.5, FontWeight.w400, 1.45, c: muted),
      labelLarge: s(14, FontWeight.w600, 1.3),
      labelMedium: s(12.5, FontWeight.w600, 1.3),
      labelSmall: s(11.5, FontWeight.w500, 1.3, c: muted, ls: 0.2),
    );
  }

  static ThemeData _build(
    ColorScheme scheme,
    Color background,
    StatusColors status,
  ) {
    final text = _text(scheme.onSurface, scheme.onSurfaceVariant);
    final fieldRadius = BorderRadius.circular(Radii.md);
    const buttonShape = StadiumBorder();
    final buttonText = text.labelLarge!.copyWith(fontSize: 15);

    return ThemeData(
      useMaterial3: true,
      fontFamily: fontFamily,
      colorScheme: scheme,
      scaffoldBackgroundColor: background,
      canvasColor: background,
      extensions: [status],
      textTheme: text,
      visualDensity: VisualDensity.standard,
      splashFactory: InkSparkle.splashFactory,
      appBarTheme: AppBarTheme(
        backgroundColor: background,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        titleSpacing: Space.page,
        toolbarHeight: 60,
        foregroundColor: scheme.onSurface,
        titleTextStyle: text.titleLarge,
      ),
      cardTheme: CardThemeData(
        color: scheme.surface,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(borderRadius: Radii.brLg),
      ),
      dividerTheme: DividerThemeData(
        color: scheme.outlineVariant,
        space: 1,
        thickness: 1,
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: scheme.surface,
        surfaceTintColor: Colors.transparent,
        indicatorColor: scheme.primaryContainer,
        indicatorShape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(Radii.md),
        ),
        elevation: 0,
        height: 66,
        labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
        labelTextStyle: WidgetStateProperty.resolveWith((states) {
          final selected = states.contains(WidgetState.selected);
          return text.labelSmall!.copyWith(
            fontSize: 12,
            fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
            color: selected ? scheme.onSurface : scheme.onSurfaceVariant,
          );
        }),
        iconTheme: WidgetStateProperty.resolveWith((states) {
          final selected = states.contains(WidgetState.selected);
          return IconThemeData(
            size: 23,
            color: selected
                ? scheme.onPrimaryContainer
                : scheme.onSurfaceVariant,
          );
        }),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: scheme.surfaceContainerHigh,
        isDense: true,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: Space.lg,
          vertical: 14,
        ),
        hintStyle: text.bodyMedium!.copyWith(
          color: scheme.onSurfaceVariant.withValues(alpha: 0.8),
        ),
        labelStyle: text.bodyMedium!.copyWith(color: scheme.onSurfaceVariant),
        floatingLabelStyle: text.labelMedium!.copyWith(color: scheme.primary),
        prefixIconColor: scheme.onSurfaceVariant,
        suffixIconColor: scheme.onSurfaceVariant,
        border: OutlineInputBorder(
          borderRadius: fieldRadius,
          borderSide: BorderSide.none,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: fieldRadius,
          borderSide: BorderSide.none,
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: fieldRadius,
          borderSide: BorderSide(color: scheme.primary, width: 1.4),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: fieldRadius,
          borderSide: BorderSide(color: scheme.error),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: fieldRadius,
          borderSide: BorderSide(color: scheme.error, width: 1.4),
        ),
        errorStyle: text.bodySmall!.copyWith(color: scheme.error),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size(0, Sizes.button),
          padding: const EdgeInsets.symmetric(horizontal: Space.xl),
          shape: buttonShape,
          textStyle: buttonText,
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size(0, Sizes.button),
          padding: const EdgeInsets.symmetric(horizontal: Space.lg),
          shape: buttonShape,
          side: BorderSide(color: scheme.outline),
          foregroundColor: scheme.onSurface,
          textStyle: buttonText,
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          minimumSize: const Size(0, Sizes.buttonCompact),
          padding: const EdgeInsets.symmetric(horizontal: Space.md),
          shape: buttonShape,
          textStyle: text.labelLarge,
        ),
      ),
      iconButtonTheme: IconButtonThemeData(
        style: IconButton.styleFrom(minimumSize: const Size(44, 44)),
      ),
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: scheme.primary,
        foregroundColor: scheme.onPrimary,
        elevation: 3,
        focusElevation: 3,
        hoverElevation: 4,
        highlightElevation: 2,
        extendedTextStyle: buttonText,
        shape: const CircleBorder(),
      ),
      chipTheme: ChipThemeData(
        shape: const StadiumBorder(),
        side: BorderSide.none,
        color: WidgetStateProperty.resolveWith(
          (st) => st.contains(WidgetState.selected)
              ? scheme.primary
              : scheme.surface,
        ),
        checkmarkColor: scheme.onPrimary,
        labelStyle: text.labelMedium!.copyWith(fontSize: 14, color: scheme.onSurface),
        secondaryLabelStyle: text.labelMedium!.copyWith(fontSize: 14, color: scheme.onPrimary),
        elevation: 0,
        padding: const EdgeInsets.symmetric(
          horizontal: Space.sm,
          vertical: Space.xs,
        ),
        labelPadding: const EdgeInsets.symmetric(horizontal: Space.xs),
        showCheckmark: false,
      ),
      segmentedButtonTheme: SegmentedButtonThemeData(
        style: ButtonStyle(
          visualDensity: VisualDensity.compact,
          textStyle: WidgetStatePropertyAll(text.labelMedium),
          side: WidgetStatePropertyAll(
            BorderSide(color: scheme.outlineVariant),
          ),
          shape: WidgetStatePropertyAll(
            RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(Radii.sm),
            ),
          ),
        ),
      ),
      listTileTheme: ListTileThemeData(
        iconColor: scheme.onSurfaceVariant,
        titleTextStyle: text.bodyLarge!.copyWith(fontWeight: FontWeight.w500),
        subtitleTextStyle: text.bodySmall,
        contentPadding: const EdgeInsets.symmetric(horizontal: Space.lg),
        minVerticalPadding: Space.sm,
      ),
      switchTheme: SwitchThemeData(
        thumbIcon: WidgetStateProperty.resolveWith(
          (s) => s.contains(WidgetState.selected)
              ? const Icon(Icons.check_rounded, size: 16)
              : null,
        ),
      ),
      checkboxTheme: CheckboxThemeData(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(Radii.xs),
        ),
        side: BorderSide(color: scheme.outline, width: 1.6),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: scheme.surface,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(Radii.xl),
        ),
        titleTextStyle: text.titleLarge,
        contentTextStyle: text.bodyMedium!.copyWith(
          color: scheme.onSurfaceVariant,
        ),
      ),
      bottomSheetTheme: BottomSheetThemeData(
        backgroundColor: scheme.surface,
        surfaceTintColor: Colors.transparent,
        showDragHandle: true,
        dragHandleColor: scheme.outline,
        dragHandleSize: const Size(36, 4),
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(Radii.xl)),
        ),
      ),
      popupMenuTheme: PopupMenuThemeData(
        color: scheme.surface,
        surfaceTintColor: Colors.transparent,
        elevation: 6,
        shape: RoundedRectangleBorder(borderRadius: Radii.brMd),
        textStyle: text.bodyMedium,
      ),
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        backgroundColor: scheme.brightness == Brightness.light
            ? const Color(0xFF1C2638)
            : const Color(0xFFE7EAF0),
        contentTextStyle: text.bodyMedium!.copyWith(
          color: scheme.brightness == Brightness.light
              ? Colors.white
              : const Color(0xFF141B2B),
        ),
        actionTextColor: scheme.brightness == Brightness.light
            ? AppColors.darkPrimary
            : AppColors.primary,
        shape: RoundedRectangleBorder(borderRadius: Radii.brMd),
        insetPadding: const EdgeInsets.fromLTRB(
          Space.page,
          0,
          Space.page,
          Space.md,
        ),
      ),
      tabBarTheme: TabBarThemeData(
        dividerHeight: 0,
        labelStyle: text.labelLarge,
        unselectedLabelStyle: text.labelLarge!.copyWith(
          fontWeight: FontWeight.w500,
        ),
        labelColor: scheme.onSurface,
        unselectedLabelColor: scheme.onSurfaceVariant,
        indicatorSize: TabBarIndicatorSize.tab,
        overlayColor: const WidgetStatePropertyAll(Colors.transparent),
      ),
      progressIndicatorTheme: ProgressIndicatorThemeData(
        color: scheme.primary,
        linearTrackColor: scheme.surfaceContainerHigh,
      ),
      pageTransitionsTheme: const PageTransitionsTheme(
        builders: {
          TargetPlatform.android: FadeForwardsPageTransitionsBuilder(),
        },
      ),
    );
  }
}
