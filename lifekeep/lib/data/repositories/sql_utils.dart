/// Escapes LIKE wildcards so user input is matched literally.
String likePattern(String q) {
  final escaped = q.replaceAll(r'\', r'\\').replaceAll('%', r'\%').replaceAll('_', r'\_');
  return '%$escaped%';
}
