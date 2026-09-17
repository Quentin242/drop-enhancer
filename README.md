# Drop Enhancer

Configurable drop popups and sounds for RuneLite.

- Collection-log unlock and repeat-drop popups.
- Rarity colours, collection statistics and configurable sounds.
- Item inclusion/exclusion rules and Ground Items value-tier audio.
- Your own WAV per rarity tier, and popup previews from the settings.

Enable **Chat**, **Pop-up**, or **Both** for collection-log notifications in the OSRS settings. With both enabled, Drop Enhancer shows one unlock notification.

For custom audio, put an uncompressed PCM `.wav` file (up to 32 MB) in `.runelite/collection-celebrations/sounds/` and enter its filename in the tier's WAV setting. Keep the default filename to use the bundled sound, or leave the field blank to mute it. The separate highlighted-item WAV falls back to the Rare sound when blank.

Open pages of your collection log to load your item quantities. These reset on logout or world hop. “Last synced” shows the last quantity read from the log; “Temporary” shows 1 for an observed unlock and 0 when ownership is unknown.

Enable **Refresh wiki completion data** to load item definitions and community completion percentages. Changes take effect immediately. Downloads contain public data, send no player information, and are cached for up to one day. These percentages are separate from your own collection quantities.

Optional files in `.runelite/collection-celebrations/data/` are loaded at startup:

- `collection-log.json`: local item definitions and completion percentages.
- `drop-rates.json`: per-source probabilities, for example `{"Source name": {"Item name": 0.00125}}`. Values must be greater than 0 and at most 1.

Neither file is required or bundled. Missing or unreadable files are ignored.

**Highlighted item sound** works with repeat popups disabled. Collection-item exclusions, collection-audio mute and tier volume settings still apply.

With Clue Case or Loot Case, recognized rewards wait at least ten seconds and remain queued while the reward interface is hidden. This estimates when the reel finishes; it does not detect the exact end. Popup and queued audio wait together. Audio already playing finishes normally. Kill counts require a matching message in the same game tick as the loot.

Based on [Collection Log Popup Enhanced](https://github.com/TimHeessels/collection-log-popup-enhanced) by SnakeSteak and [Custom Sounds](https://github.com/daanbom/Custom-Drop-Sounds) by daanbom. Integration by maiz. RuneLite provides client assets; optional collection data credits OSRS Wiki contributors.

Source code: [BSD-2-Clause](LICENSE). Full credits and upstream licenses: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Rare, Very rare and collection-log sounds courtesy of **daanbom / Custom Sounds**, used with permission. Common, Uncommon and Pet use original synthesized sounds. See [audio terms](AUDIO-PERMISSION.md).
