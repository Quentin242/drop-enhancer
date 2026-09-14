"""Original notification synthesis; BSD-2-Clause. Requires NumPy, no input samples.

Run manually from any directory; never invoked by the plugin or its build.
"""
from pathlib import Path
import hashlib
import json
import wave
import numpy as np

RATE = 44100
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources/sounds"


def voice(note, duration, choir=False, seed=0, articulated=False):
    t = np.arange(round(duration * RATE)) / RATE
    frequency = 440 * 2 ** ((note - 69) / 12)
    signal = np.zeros_like(t)
    if choir:
        # Detuned singers with vowel formants, slow vibrato and no bell layer.
        rng = np.random.default_rng(seed)
        for cents in (-7, -2, 3, 8):
            f = frequency * 2 ** (cents / 1200)
            phase = 2 * np.pi * f * t + .11 * np.sin(2 * np.pi * 4.6 * t + rng.uniform(0, 6))
            for harmonic in range(1, 25):
                hz = harmonic * f
                formant = .25 + 1.8 * np.exp(-.5 * ((hz - 700) / 200) ** 2)
                formant += 1.0 * np.exp(-.5 * ((hz - 1150) / 300) ** 2)
                formant += 1.4 * np.exp(-.5 * ((hz - 2500) / 600) ** 2)
                gain = formant * np.exp(-hz / 4600) / harmonic
                signal += gain * np.sin(harmonic * phase + rng.uniform(0, 6)) / 4
        attack, release = .65, 1.7
    else:
        # More brass presence from upper partials, retaining a rounded attack.
        f = frequency * (1 - .018 * np.exp(-t / .035))
        phase = 2 * np.pi * np.cumsum(f) / RATE
        bloom = (1 - np.exp(-t / .045)) * np.exp(-t * .7)
        signal = .85 * np.sin(phase) + .52 * bloom * np.sin(2 * phase) + .30 * bloom * np.sin(3 * phase)
        signal += bloom * (.17 * np.sin(4 * phase) + .10 * np.sin(5 * phase) + .055 * np.sin(6 * phase))
        attack, release = .028, min(.22, duration * .35)
        if articulated:
            attack, release = .012, min(.06, duration * .18)
    envelope = np.sin(np.minimum(1, t / attack) * np.pi / 2) ** 2
    envelope *= np.sin(np.minimum(1, (duration - t) / release) * np.pi / 2) ** 2
    return signal * envelope


def render(filename, duration, notes, choir=False, articulated=False):
    track = np.zeros((round(duration * RATE), 2))
    for index, (note, start, length, gain, pan) in enumerate(notes):
        sound = voice(note, length, choir, index, articulated) * gain
        offset = round(start * RATE)
        track[offset:offset + len(sound)] += sound[:, None] * np.sqrt([(1 - pan) / 2, (1 + pan) / 2])
    dry = track.copy()
    delays = [(.113, .12), (.227, .09), (.379, .06), (.557, .04)] if choir else [(.071, .055), (.139, .025)]
    if articulated:
        # Keep the pickup's mini-rest audible instead of filling it with echoes.
        delays = []
    for seconds, gain in delays:
        n = round(seconds * RATE)
        track[n:] += dry[:-n, ::-1] * gain
    track[-round(.15 * RATE):] *= np.linspace(1, 0, round(.15 * RATE))[:, None] ** 2
    track *= (.38 if choir else .42) / np.max(np.abs(track))
    pcm = np.rint(track * 32767).astype('<i2')
    path = OUT / filename
    with wave.open(str(path), 'wb') as output:
        output.setnchannels(2)
        output.setsampwidth(2)
        output.setframerate(RATE)
        output.writeframes(pcm.tobytes())
    return {"file": filename, "source": "Original procedural synthesis; scripts/generate-soft-sounds.py. No input recordings or samples.",
            "sha256": hashlib.sha256(path.read_bytes()).hexdigest(), "durationSeconds": duration,
            "changes": "Stereo 44.1 kHz 16-bit PCM; " + ("Common-register brass: 160 ms pickup, 65 ms rest, 130 ms short note directly joined to a 0.95-second held ending with a short 60 ms fade-out." if articulated else "slow vowel-formant choir swell with soprano voices, lighter bass and clearer upper formants, no chimes." if choir else f"clearer {len(notes)}-note brass fanfare with rounded attack and added upper harmonics."),
            "license": "BSD-2-Clause; see LICENSE."}


records = [
    render("custom-sounds-common.wav", 1.18, [(60, 0, .205, .8, 0), (67, .195, .80, .8, 0)]),
    # Extend Common's C-to-G motif in the same register, with an E pickup in between.
    # Puu, mini-rest, short pwa directly into pwaaaaaaaa (10 ms envelope overlap).
    render("drop-enhancer-uncommon.wav", 1.42, [(60, 0, .16, .8, 0), (64, .225, .13, .8, 0), (67, .345, .95, .8, 0)], articulated=True),
    render("drop-enhancer-pet.wav", 4.6, [(48, 0, 3.7, .15, -.4), (55, .07, 3.7, .16, .4),
                                        (60, .1, 3.7, .32, -.6), (64, .16, 3.7, .30, .6), (67, .2, 3.7, .28, 0),
                                        (72, .12, 3.7, .30, -.3), (76, .22, 3.7, .24, .3)], True),
]
manifest = ROOT / "src/main/resources/META-INF/sound-provenance.json"
data = json.loads(manifest.read_text())
replacements = {entry['file']: entry for entry in records}
data['files'] = [replacements.get(entry['file'], entry) for entry in data['files']]
manifest.write_text(json.dumps(data, indent=2) + '\n')
print(json.dumps(records, indent=2))
