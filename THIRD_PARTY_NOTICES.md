# Source provenance

Custom Sounds: copyright (c) 2023 daanbom, BSD-2-Clause.
https://github.com/daanbom/Custom-Drop-Sounds/tree/0907827d2440b270576713cbd84a749d9cb7e213
Adapted drop configuration and sound/event selection into CustomSoundEvents and CelebrationConfig. Non-drop features and weapon mappings are not included. Replaced playback, scheduling and lifecycle management. Five default recordings are now supplied from Custom Sounds with author permission; see AUDIO-PERMISSION.md for their separate terms and exact sources. Full terms: licenses/custom-sounds.txt.

Collection Log Popup Enhanced: copyright (c) 2026 SnakeSteak, BSD-2-Clause.
https://github.com/TimHeessels/collection-log-popup-enhanced/tree/b791be9b48ff0ad83b962fe9e0bfd2bcde368995
Adapted KillCountTracker, KillCountKind, collection-unlock matching, native popup paint suppression, and the original RarityResolver, RarityResult, RarityTier, RarityBasis, DropRateResolver and CollectionLogSlotNames. Tradeable items retain the upstream rarity weighting, percentile thresholds, gold thresholds and fallbacks; untradeable items use completion rarity with available drop-rate fallback and exclude value; preview reuses a single distribution per click. Adapted popup layout, RuneScape font rendering and separate item-icon animation in CelebrationOverlay; all panel frame geometry is original. Added KC freshness checks. Original bitmap overlay resources and sounds are excluded. Wiki completion data is fetched separately from the wiki and cached locally; source-specific drop rates can be loaded from user-local files. Neither dataset is distributed in this JAR. Full terms: licenses/enhanced.txt.

New code and original Java2D panel: copyright (c) 2026 maiz, BSD-2-Clause, see LICENSE.

RuneLite is used as a provided client/API dependency. Its standard transitive runtime dependencies are not repackaged in this plugin jar. Five default WAVs are supplied by daanbom / Custom Sounds with author permission reported by maiz on 2026-09-14. The previously permitted UniversalAudioSFX pet recording is retained unchanged. Audio permission is separate from the BSD code license and grants no standalone reuse rights. See AUDIO-PERMISSION.md (also bundled as META-INF/AUDIO-PERMISSION.md) and META-INF/sound-provenance.json for source revisions, changes and SHA-256 hashes.

The collection-log synchronization pattern was inspected in RuneProfile (703e13108b60e5a75ffeef3f428b8500091d3dc6, CollectionLogWidgetSubscriber) as API usage reference. This plugin implements a passive reader and does not copy its automatic menu-action logic or use its network service.

Runtime wiki data: OSRS Wiki contributors, CC BY-NC-SA 3.0 (not BSD).
Sources: https://oldschool.runescape.wiki/w/Module:Collection_log/data.json and https://oldschool.runescape.wiki/w/Module:Collection_log/completion.json .
License: https://creativecommons.org/licenses/by-nc-sa/3.0/ ; policy: https://meta.weirdgloop.org/w/Licensing .
Transformation: join public item records and completion percentages by item ID; omit scores not present in the completion source. Attribution and license links are preserved in the local cache. No wiki artwork is downloaded.
