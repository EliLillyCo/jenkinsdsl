package com.lilly.cirrus.jenkinsdsl.container

class Label {
  static int LENGTH_LIMIT = 63
  
  String baseText
  private String uuid
  
  @Override
  String toString() {
    return formatLabelForKubernetesObeyingTheLengthLimit(this.@baseText, false)
  }
  
  String toString(String fixedStringPart) {
    return formatLabelForKubernetesObeyingTheLengthLimit(this.@baseText, fixedStringPart, false)
  }
  
  String toUniqueString() {
    return toUniqueString(false)
  }
  
  String toUniqueString(boolean keepEndOfLabel) {
    // Once a UUID is generated, the same UUID will be used by this method.
    // If you need another unique label with the same base text, create another Label object.
    if (!this.@uuid) {
      this.@uuid = UUID.randomUUID().toString()
    }
    return formatLabelForKubernetesObeyingTheLengthLimit(this.@baseText, this.@uuid, keepEndOfLabel)
  }
  
  private String formatLabelForKubernetesObeyingTheLengthLimit(String label, boolean keepEndOfLabel) {
    return clean(label, LENGTH_LIMIT, keepEndOfLabel)
  }
  
  private String formatLabelForKubernetesObeyingTheLengthLimit(String baseLabel, String fixedStringPart, boolean keepEndOfLabel) {
    int allowedLabelLength = computeAllowedLabelLength(fixedStringPart)
    String cleanLabel = clean(baseLabel, allowedLabelLength, keepEndOfLabel)
    return labelWithFixedStringPart(cleanLabel, fixedStringPart)
  }
  
  private String clean(String label, int allowedLabelLength, boolean keepEndOfLabel) {
    String possiblyTruncatedLabel = possiblyTruncateLabel(label, allowedLabelLength, keepEndOfLabel)
    String labelWithoutSpaces = replaceSpacesWithHyphens(possiblyTruncatedLabel)
    String labelWithAllowedCharacters = removeDisallowedCharacters(labelWithoutSpaces)
    return labelWithAllowedCharacters.toLowerCase()
  }
  
  private String replaceSpacesWithHyphens(String label) {
    label.replaceAll(' ', '-')
  }
  
  private String removeDisallowedCharacters(String label) {
    String disallowedRemoved = label.replaceAll('[^A-Za-z0-9\\-\\.]', '')
    if (disallowedRemoved.matches('^[^a-z].*')) {
      disallowedRemoved = 'a' + disallowedRemoved.substring(1)
    }
    if (disallowedRemoved.matches('.*[^a-z]$')) {
      disallowedRemoved = disallowedRemoved.substring(0, disallowedRemoved.length() - 1) + 'a'
    }
    return disallowedRemoved
  }
  
  private int computeAllowedLabelLength(String fixedString) {
    LENGTH_LIMIT - fixedString.size() - 1
  }
  
  private String possiblyTruncateLabel(String label, int allowedLabelLength, boolean keepEndOfLabel) {
    String possiblyTruncatedLabel
    if (keepEndOfLabel) {
      possiblyTruncatedLabel = label.size() > allowedLabelLength ? label[-allowedLabelLength..-1] : label
    } else {
      possiblyTruncatedLabel = label.take(allowedLabelLength)
    }
    return possiblyTruncatedLabel
  }
  
  private String labelWithFixedStringPart(String label, String fixedString) {
    return "${label}-${fixedString}".toString()
  }
}
