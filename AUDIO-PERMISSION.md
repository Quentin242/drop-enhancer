# Bundled audio permission and provenance

The six WAVs in `src/main/resources/sounds/` come from the locally identified **UniversalAudioSFX** pack. On 2026-09-14 the maintainer, maiz, reported that they had checked with the author and that these sounds may be used and pushed for now, with removal required if that permission changes.

This records the maintainer's report; it is not a quotation of the author's message. The author's public name, pack URL and any requested exact credit wording are awaiting confirmation from the maintainer. Complete those attribution details before publishing this candidate.

The audio remains owned by its original rights holder. The repository's BSD-2-Clause license applies to the source code, not these audio files. No permission for standalone reuse, extraction into another product or redistribution of the original pack is granted by this notice.

## Included selection

The original pack paths below are relative to `UniversalAudioSFX/`. The `botssouls-` prefix preserves existing user settings; it does not identify the audio author. Edits were made for this plugin, with the original pack files retained unchanged.

| Bundled file | Original pack file | Seconds |
| --- | --- | --- |
| `botssouls-common.wav` | `RewardsAndLootsSoundEffects/ReceiveItem01/SFX_ReceiveItemUIv2.wav` | 0.24 |
| `botssouls-uncommon.wav` | `RewardsAndLootsSoundEffects/PickItem01/SFX_PickItemv1.wav` | 0.85 |
| `botssouls-rare.wav` | `RewardsAndLootsSoundEffects/FindCrystal01/SFX_FindCrystalCompleteMusic02v1.wav` | 2.43 |
| `botssouls-veryrare.wav` | `RewardsAndLootsSoundEffects/LevelUp01/SFX_LevelUpv3.wav` | 3.234 |
| `botssouls-pet.wav` | `RewardsAndLootsSoundEffects/FindEpicItem02/SFX_FindEpicItemNoWhooshMusic01v1.wav` | 5.875 |
| `botssouls-unlock.wav` | `RewardsAndLootsSoundEffects/ReceiveItem01/SFX_ReceiveItemMusic02v1.wav` | 1.1 |

Exact changes and SHA-256 hashes are recorded in [sound-provenance.json](src/main/resources/META-INF/sound-provenance.json), bundled at `META-INF/sound-provenance.json` in the JAR. Local user WAVs override bundled defaults; invalid overrides are silent. Missing custom filenames do not select an unrelated default.

## If permission changes

1. Remove the six WAVs listed in the manifest from `src/main/resources/sounds/` and remove them from release artifacts under the maintainer's control.
2. Set the six default filename methods in `CelebrationConfig` to blank, or replace them with separately cleared sounds. Existing saved filenames may remain; missing resources are silent. Do not delete user-imported files.
3. Update this notice, the manifest, README, third-party notices and their `META-INF` copies. Adjust the bundled-sound regression test for the replacement or silent defaults.
4. Run clean-checkout tests and the standard Plugin Hub build, publish the replacement source commit and update any Plugin Hub submission marker.

Removing a file in a later commit does not remove it from earlier Git history or existing downloads. Coordinate historical artifact removal separately if required; do not promise that already distributed copies can be recalled.
