import 'package:flutter/material.dart';

import 'fancy.dart';

import 'package:provider/provider.dart';

import '../../core/l10n/lang.dart';
import '../../core/theme/app_theme.dart';
import '../../core/theme/tokens.dart';
import '../../core/utils/errors.dart';
import '../../state/app_controller.dart';

/// Runs [action]; shows a friendly localized message on failure (never a
/// stack trace) and an optional [success] message.
Future<bool> runAction(
  BuildContext context,
  Future<void> Function() action, {
  String? success,
}) async {
  final messenger = ScaffoldMessenger.maybeOf(context);
  final errorColor = Theme.of(context).colorScheme.error;
  try {
    await action();
    if (success != null) {
      messenger?.hideCurrentSnackBar();
      messenger?.showSnackBar(SnackBar(content: Text(success)));
    }
    return true;
  } catch (e) {
    messenger?.hideCurrentSnackBar();
    messenger?.showSnackBar(
      SnackBar(content: Text(friendlyError(e)), backgroundColor: errorColor),
    );
    return false;
  }
}

void showSnack(
  BuildContext context,
  String message, {
  bool error = false,
  SnackBarAction? action,
}) {
  final m = ScaffoldMessenger.maybeOf(context);
  m?.hideCurrentSnackBar();
  m?.showSnackBar(
    SnackBar(
      content: Text(message),
      action: action,
      backgroundColor: error ? Theme.of(context).colorScheme.error : null,
    ),
  );
}

Future<bool> confirmDialog(
  BuildContext context, {
  required String title,
  required String message,
  required String confirmLabel,
  bool destructive = false,
  IconData? icon,
}) async {
  final scheme = Theme.of(context).colorScheme;
  final r = await showDialog<bool>(
    context: context,
    builder: (ctx) => AlertDialog(
      icon: icon == null
          ? null
          : Icon(icon, color: destructive ? scheme.error : scheme.primary),
      title: Text(title),
      content: Text(message),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(ctx, false),
          child: Text(ctx.tr('ยกเลิก', 'Cancel')),
        ),
        FilledButton(
          style: destructive
              ? FilledButton.styleFrom(
                  backgroundColor: scheme.error,
                  foregroundColor: scheme.onError,
                  minimumSize: const Size(0, Sizes.buttonCompact),
                )
              : FilledButton.styleFrom(
                  minimumSize: const Size(0, Sizes.buttonCompact),
                ),
          onPressed: () => Navigator.pop(ctx, true),
          child: Text(confirmLabel),
        ),
      ],
    ),
  );
  return r == true;
}

/// One option in [showOptionSheet].
class SheetOption<T> {
  const SheetOption({
    required this.value,
    required this.label,
    required this.icon,
    this.subtitle,
    this.color,
  });
  final T value;
  final String label;
  final String? subtitle;
  final IconData icon;
  final Color? color;
}

/// Bottom sheet with a list of options; returns the chosen value.
Future<T?> showOptionSheet<T>(
  BuildContext context, {
  String? title,
  required List<SheetOption<T>> options,
}) {
  return showModalBottomSheet<T>(
    context: context,
    builder: (ctx) {
      final t = Theme.of(ctx).textTheme;
      final scheme = Theme.of(ctx).colorScheme;
      return SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(Space.sm, 0, Space.sm, Space.sm),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (title != null)
                Padding(
                  padding: const EdgeInsets.fromLTRB(
                    Space.md,
                    0,
                    Space.md,
                    Space.sm,
                  ),
                  child: Text(title, style: t.titleMedium),
                ),
              for (final o in options)
                ListTile(
                  shape: RoundedRectangleBorder(borderRadius: Radii.brMd),
                  leading: Icon(o.icon, color: o.color ?? scheme.primary),
                  title: Text(
                    o.label,
                    style: t.bodyLarge?.copyWith(
                      fontWeight: FontWeight.w500,
                      color: o.color,
                    ),
                  ),
                  subtitle: o.subtitle == null ? null : Text(o.subtitle!),
                  onTap: () => Navigator.pop(ctx, o.value),
                ),
            ],
          ),
        ),
      );
    },
  );
}

/// Loads data with [load], re-runs when app data changes ([AppController.revision])
/// or [deps] change, and keeps the previous result on screen while reloading.
class DataBuilder<T> extends StatefulWidget {
  const DataBuilder({
    super.key,
    required this.load,
    required this.builder,
    this.deps = const [],
    this.loading,
  });

  final Future<T> Function(AppController c) load;
  final Widget Function(BuildContext context, T data) builder;
  final List<Object?> deps;
  final Widget? loading;

  @override
  State<DataBuilder<T>> createState() => _DataBuilderState<T>();
}

class _DataBuilderState<T> extends State<DataBuilder<T>> {
  T? _data;
  Object? _error;
  int _rev = -1;
  List<Object?> _deps = const [];
  int _ticket = 0;

  bool _depsChanged() {
    if (_deps.length != widget.deps.length) return true;
    for (var i = 0; i < _deps.length; i++) {
      if (_deps[i] != widget.deps[i]) return true;
    }
    return false;
  }

  Future<void> _reload(AppController c) async {
    final ticket = ++_ticket;
    try {
      final d = await widget.load(c);
      if (!mounted || ticket != _ticket) return;
      setState(() {
        _data = d;
        _error = null;
      });
    } catch (e) {
      if (!mounted || ticket != _ticket) return;
      setState(() => _error = e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.watch<AppController>();
    if (c.revision != _rev || _depsChanged()) {
      _rev = c.revision;
      _deps = List.of(widget.deps);
      WidgetsBinding.instance.addPostFrameCallback((_) => _reload(c));
    }
    if (_error != null && _data == null) {
      return ErrorView(
        message: friendlyError(_error!),
        onRetry: () => _reload(c),
      );
    }
    final d = _data;
    if (d == null) {
      return widget.loading ??
          const Center(
            child: Padding(
              padding: EdgeInsets.all(Space.xxxl),
              child: SizedBox(
                width: 26,
                height: 26,
                child: CircularProgressIndicator(strokeWidth: 2.5),
              ),
            ),
          );
    }
    return widget.builder(context, d);
  }
}

class ErrorView extends StatelessWidget {
  const ErrorView({super.key, required this.message, this.onRetry});
  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    return EmptyState(
      icon: Icons.error_outline_rounded,
      title: context.tr('เกิดข้อผิดพลาด', 'Something went wrong'),
      message: message,
      actionLabel: onRetry == null ? null : context.tr('ลองใหม่', 'Retry'),
      actionIcon: Icons.refresh_rounded,
      onAction: onRetry,
      tone: EmptyTone.error,
    );
  }
}

enum EmptyTone { neutral, error, success }

class EmptyState extends StatelessWidget {
  const EmptyState({
    super.key,
    required this.icon,
    required this.title,
    this.message,
    this.actionLabel,
    this.actionIcon = Icons.add_rounded,
    this.onAction,
    this.compact = false,
    this.tone = EmptyTone.neutral,
    this.illustration,
  });

  /// Asset picture shown instead of the icon box.
  final String? illustration;
  final IconData icon;
  final String title;
  final String? message;
  final String? actionLabel;
  final IconData actionIcon;
  final VoidCallback? onAction;
  final bool compact;
  final EmptyTone tone;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    final (Color fg, Color bg) = switch (tone) {
      EmptyTone.error => (scheme.error, context.status.dangerContainer),
      EmptyTone.success => (
        context.status.success,
        context.status.successContainer,
      ),
      EmptyTone.neutral => (
        scheme.primary,
        scheme.primaryContainer.withValues(alpha: 0.7),
      ),
    };
    return Center(
      child: Padding(
        padding: EdgeInsets.symmetric(
          horizontal: Space.xxxl,
          vertical: compact ? Space.xl : 56,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (illustration != null)
              SoftIllustration(
                asset: illustration!,
                height: compact ? 150 : 210,
              )
            else
              Container(
                width: compact ? 48 : 64,
                height: compact ? 48 : 64,
                decoration: BoxDecoration(
                  color: bg,
                  borderRadius: BorderRadius.circular(compact ? 16 : 22),
                ),
                child: Icon(icon, size: compact ? 24 : 30, color: fg),
              ),
            SizedBox(height: compact ? Space.md : Space.lg),
            Text(
              title,
              style: illustration != null
                  ? t.headlineSmall?.copyWith(fontSize: 26)
                  : t.titleMedium,
              textAlign: TextAlign.center,
            ),
            if (message != null) ...[
              const SizedBox(height: Space.xs),
              Text(
                message!,
                style: t.bodyMedium?.copyWith(color: scheme.onSurfaceVariant),
                textAlign: TextAlign.center,
              ),
            ],
            if (actionLabel != null &&
                onAction != null &&
                illustration != null) ...[
              const SizedBox(height: Space.xl),
              SizedBox(
                width: 260,
                child: GradientButton(
                  label: actionLabel!,
                  icon: actionIcon,
                  onPressed: onAction!,
                ),
              ),
            ] else if (actionLabel != null && onAction != null) ...[
              const SizedBox(height: Space.lg),
              FilledButton.icon(
                style: FilledButton.styleFrom(
                  minimumSize: const Size(0, Sizes.buttonCompact),
                ),
                onPressed: onAction,
                icon: Icon(actionIcon, size: 20),
                label: Text(actionLabel!),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

/// Colour for task priority (0 low, 1 normal, 2 high).
Color priorityColor(BuildContext context, int index) {
  switch (index) {
    case 2:
      return context.status.danger;
    case 0:
      return Theme.of(context).colorScheme.secondary;
    default:
      return context.status.warning;
  }
}
