# Drop Enhancer

Configurable drop popups and sounds for RuneLite.

- Collection-log unlock and repeat-drop popups.
- Rarity colours, collection statistics and configurable sounds.
- Item inclusion/exclusion rules and Ground Items value-tier audio.
- Custom WAV selection and popup previews.

To receive new-unlock notifications, enable **Chat**, **Pop-up**, or **Both** for collection-log notifications in the OSRS settings. Drop Enhancer recognizes both notification forms and combines them into one unlock. Unrecognized popup text is left visible.

Open the relevant pages of your own collection log to synchronize official quantities. These observations reset on logout or world hop. “Last synced” is a previous official quantity; “Temporary” is only whether an unlock has been observed, not a lifetime total. Optional **Refresh wiki completion data** loads public item definitions and population statistics when enabled, without restarting the plugin. It sends no player data and reuses a cache for up to one day. Wiki statistics do not establish your own collection quantities.

Two optional files in `.runelite/collection-celebrations/data/` are read at startup if you put them there. `collection-log.json` is a local copy of the same item/completion dataset the wiki refresh fetches, for use with the refresh turned off. `drop-rates.json` adds per-source drop rates, shown as “1/x” on the popup, in the shape `{"Source name": {"Item name": 0.00125}}` with probabilities between 0 and 1. Neither file is shipped with the plugin and neither is required; missing or unreadable files are ignored.

**Highlighted item sound** also works when repeat popups are disabled. Explicit item exclusions, collection-audio mute and the selected sound's volume still apply to collection drops.

Clue Case and Loot Case compatibility uses public overlay names, hidden reward interfaces and a minimum ten-second hold for recognized rewards. This is a compatibility heuristic, not exact detection of the end of a reel. Popup and queued reward audio wait together. Started audio finishes normally. Kill counts are shown only when a matching message is observed in the loot's game tick; a missing count is left unavailable.

Based on [Collection Log Popup Enhanced](https://github.com/TimHeessels/collection-log-popup-enhanced) by SnakeSteak and [Custom Sounds](https://github.com/daanbom/Custom-Drop-Sounds) by daanbom. Integration by maiz. RuneLite provides client assets; optional collection data credits OSRS Wiki contributors.

Source code: [BSD-2-Clause](LICENSE). Full credits and upstream licenses: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Rare, Very rare and collection-log sounds courtesy of **daanbom / Custom Sounds**, used with permission. Common, Uncommon and Pet use original synthesized sounds. See [audio terms](AUDIO-PERMISSION.md).
