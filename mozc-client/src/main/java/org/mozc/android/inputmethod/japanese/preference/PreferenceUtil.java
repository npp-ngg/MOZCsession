// Copyright 2010-2018, Google Inc.
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package org.mozc.android.inputmethod.japanese.preference;

import com.google.common.base.Optional;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceManager;

/**
 * Utilities for Mozc preferences.
 *
 */
public class PreferenceUtil {

  /** Simple {@code PreferenceManager} wrapper for testing purpose. */
  interface PreferenceManagerInterface {
    public Preference findPreference(CharSequence key);
  }

  /** Simple {@code PreferenceManager} wrapper for testing purpose.
   *  This interface wraps static method so no constructor is required.
   */
  public interface PreferenceManagerStaticInterface {
    public void setDefaultValues(Context context, int id, boolean readAgain);
  }

  private static Optional<PreferenceManagerStaticInterface> defaultPreferenceManagerStatic =
      Optional.absent();

  // Keys for Keyboard Layout.
  public static final String PREF_CURRENT_KEYBOARD_LAYOUT_KEY =
      "pref_current_keyboard_layout_key";

  public static final String PREF_SOFTWARE_KEYBOARD_ADVANED_PORTRAIT_KEY =
      "pref_software_keyboard_advanced_portrait_key";
  public static final String PREF_PORTRAIT_KEYBOARD_LAYOUT_KEY =
      "pref_portrait_keyboard_layout_key";
  public static final String PREF_PORTRAIT_INPUT_STYLE_KEY =
      "pref_portrait_input_style_key";
  public static final String PREF_PORTRAIT_QWERTY_LAYOUT_FOR_ALPHABET_KEY =
      "pref_portrait_qwerty_layout_for_alphabet_key";
  public static final String PREF_PORTRAIT_FLICK_SENSITIVITY_KEY =
      "pref_portrait_flick_sensitivity_key";
  public static final String PREF_PORTRAIT_LAYOUT_ADJUSTMENT_KEY =
      "pref_portrait_layout_adjustment_key";
  public static final String PREF_PORTRAIT_KEYBOARD_HEIGHT_RATIO_KEY =
      "pref_portrait_keyboard_height_ratio_key";

  public static final String PREF_SOFTWARE_KEYBOARD_ADVANED_LANDSCAPE_KEY =
      "pref_software_keyboard_advanced_landscape_key";
  public static final String PREF_LANDSCAPE_KEYBOARD_LAYOUT_KEY =
      "pref_landscape_keyboard_layout_key";
  public static final String PREF_LANDSCAPE_INPUT_STYLE_KEY =
      "pref_landscape_input_style_key";
  public static final String PREF_LANDSCAPE_QWERTY_LAYOUT_FOR_ALPHABET_KEY =
      "pref_landscape_qwerty_layout_for_alphabet_key";
  public static final String PREF_LANDSCAPE_FLICK_SENSITIVITY_KEY =
      "pref_landscape_flick_sensitivity_key";
  public static final String PREF_LANDSCAPE_LAYOUT_ADJUSTMENT_KEY =
      "pref_landscape_layout_adjustment_key";
  public static final String PREF_LANDSCAPE_KEYBOARD_HEIGHT_RATIO_KEY =
      "pref_landscape_keyboard_height_ratio_key";

  public static final String PREF_USE_PORTRAIT_KEYBOARD_SETTINGS_FOR_LANDSCAPE_KEY =
      "pref_use_portrait_keyboard_settings_for_landscape_key";

  // Full screen keys.
  public static final String PREF_PORTRAIT_FULLSCREEN_KEY =
      "pref_portrait_fullscreen_key";
  public static final String PREF_LANDSCAPE_FULLSCREEN_KEY =
      "pref_landscape_fullscreen_key";

  // Keys for generic preferences.
  public static final String PREF_HARDWARE_KEYMAP = "pref_hardware_keymap";
  public static final String PREF_VOICE_INPUT_KEY = "pref_voice_input_key";
  public static final String PREF_HAPTIC_FEEDBACK_KEY = "pref_haptic_feedback_key";
  public static final String PREF_HAPTIC_FEEDBACK_DURATION_KEY =
      "pref_haptic_feedback_duration_key";
  public static final String PREF_SOUND_FEEDBACK_KEY = "pref_sound_feedback_key";
  public static final String PREF_SOUND_FEEDBACK_VOLUME_KEY =
      "pref_sound_feedback_volume_key";
  public static final String PREF_POPUP_FEEDBACK_KEY = "pref_popup_feedback_key";
  public static final String PREF_SPACE_CHARACTER_FORM_KEY =
      "pref_space_character_form_key";
  public static final String PREF_KANA_MODIFIER_INSENSITIVE_CONVERSION_KEY =
      "pref_kana_modifier_insensitive_conversion";
  public static final String PREF_TYPING_CORRECTION_KEY =
      "pref_typing_correction";
  public static final String PREF_EMOJI_PROVIDER_TYPE =
      "pref_emoji_provider_type";
  public static final String PREF_DICTIONARY_PERSONALIZATION_KEY =
      "pref_dictionary_personalization_key";
  public static final String PREF_DICTIONARY_USER_DICTIONARY_TOOL_KEY =
      "pref_dictionary_user_dictionary_tool_key";
  public static final String PREF_OTHER_INCOGNITO_MODE_KEY = "pref_other_anonimous_mode_key";
  public static final String PREF_OTHER_USAGE_STATS_KEY = "pref_other_usage_stats_key";
  public static final String PREF_ABOUT_VERSION = "pref_about_version";
  public static final String PREF_LAUNCHER_ICON_VISIBILITY_KEY = "pref_launcher_icon_visibility";
  // Application lifecycle
  public static final String PREF_LAST_LAUNCH_ABI_INDEPENDENT_VERSION_CODE =
      "pref_last_launch_abi_independent_version_code";

}