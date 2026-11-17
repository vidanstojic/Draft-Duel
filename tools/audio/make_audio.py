"""Synthesize Draft Duel's music loops and sound effects.

Everything is generated from scratch here (oscillators, noise, plucked strings), so the audio is
original and free of third-party licenses. The match music (music_match.mp3, "Tactical Rounds")
is not generated here; the theme loops below are kept as sketches but no longer written. Output goes to composeResources/files/audio as MP3,
which both Android (MediaPlayer, SoundPool) and iOS (AVAudioPlayer) play.

Usage (from the repo root; needs numpy, scipy and ffmpeg with libmp3lame):
    python tools/audio/make_audio.py
"""

from __future__ import annotations

import subprocess
import sys
import tempfile
from pathlib import Path

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, lfilter

SR = 44_100
ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "composeApp/src/commonMain/composeResources/files/audio"
rng = np.random.default_rng(7)


# ----------------------------------------------------------------------------- building blocks

def t_axis(seconds: float) -> np.ndarray:
    return np.arange(int(seconds * SR)) / SR


def note_hz(name: str) -> float:
    """'A4' -> 440; sharps only (C#4)."""
    names = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]
    pitch, octave = name[:-1], int(name[-1])
    return 440.0 * 2 ** ((names.index(pitch) + 12 * (octave + 1) - 69) / 12)


def env(n: int, a: float, d: float, s: float, r: float) -> np.ndarray:
    """ADSR over n samples; a, d, r in seconds, s level. Release takes the tail."""
    a_n, d_n, r_n = int(a * SR), int(d * SR), int(r * SR)
    s_n = max(n - a_n - d_n - r_n, 0)
    e = np.concatenate([
        np.linspace(0, 1, a_n, endpoint=False),
        np.linspace(1, s, d_n, endpoint=False),
        np.full(s_n, s),
        np.linspace(s, 0, r_n),
    ])
    return np.pad(e, (0, max(n - len(e), 0)))[:n]


def lowpass(x: np.ndarray, hz: float, order: int = 2) -> np.ndarray:
    b, a = butter(order, hz / (SR / 2), "low")
    return lfilter(b, a, x)


def highpass(x: np.ndarray, hz: float, order: int = 2) -> np.ndarray:
    b, a = butter(order, hz / (SR / 2), "high")
    return lfilter(b, a, x)


def saw(f: float, t: np.ndarray, harmonics: int = 12) -> np.ndarray:
    return sum(np.sin(2 * np.pi * f * k * t) / k for k in range(1, harmonics + 1) if f * k < SR / 2.2) * 0.6


def epiano(f: float, dur: float) -> np.ndarray:
    t = t_axis(dur)
    mod = np.sin(2 * np.pi * f * 14 * t) * 1.8 * np.exp(-t * 6)
    tone = np.sin(2 * np.pi * f * t + mod) + 0.25 * np.sin(2 * np.pi * f * 2 * t)
    return tone * env(len(t), 0.004, 0.6, 0.35, min(0.4, dur / 2)) * np.exp(-t * 0.9)


def pluck(f: float, dur: float, brightness: float = 0.5) -> np.ndarray:
    """Karplus-Strong plucked string (nylon guitar)."""
    n = int(dur * SR)
    period = int(SR / f)
    buf = rng.uniform(-1, 1, period)
    buf = lowpass(buf, 2_000 + 5_000 * brightness, 1)
    out = np.empty(n)
    for i in range(n):
        out[i] = buf[i % period]
        buf[i % period] = 0.497 * (buf[i % period] + buf[(i + 1) % period])
    return out * env(n, 0.002, 0.05, 1.0, 0.05)


def pad(freqs: list[float], dur: float, cutoff: float = 1_800) -> np.ndarray:
    t = t_axis(dur)
    x = sum(saw(f * d, t, 8) for f in freqs for d in (0.997, 1.003))
    return lowpass(x, cutoff) * env(len(t), 0.4, 0.3, 0.8, 0.6) / len(freqs)


def bass(f: float, dur: float, bright: float = 900) -> np.ndarray:
    t = t_axis(dur)
    x = np.sin(2 * np.pi * f * t) + 0.4 * lowpass(saw(f, t, 6), bright)
    return x * env(len(t), 0.005, 0.1, 0.7, 0.05)


def kick(dur: float = 0.35) -> np.ndarray:
    t = t_axis(dur)
    f = 45 + 90 * np.exp(-t * 30)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 9)


def snare(dur: float = 0.22) -> np.ndarray:
    t = t_axis(dur)
    noise = highpass(rng.uniform(-1, 1, len(t)), 1_200) * np.exp(-t * 18)
    return 0.7 * noise + 0.4 * np.sin(2 * np.pi * 190 * t) * np.exp(-t * 25)


def hat(dur: float = 0.06, open_: bool = False) -> np.ndarray:
    t = t_axis(0.25 if open_ else dur)
    return highpass(rng.uniform(-1, 1, len(t)), 7_000) * np.exp(-t * (12 if open_ else 60)) * 0.5


def clap(dur: float = 0.25) -> np.ndarray:
    t = t_axis(dur)
    noise = lowpass(highpass(rng.uniform(-1, 1, len(t)), 900), 5_000)
    e = sum(np.exp(-np.clip(t - k * 0.011, 0, None) * 140) * (t >= k * 0.011) for k in range(3))
    return noise * (e + 0.5 * np.exp(-t * 14))


def shaker(dur: float = 0.09) -> np.ndarray:
    t = t_axis(dur)
    return highpass(rng.uniform(-1, 1, len(t)), 5_000) * np.sin(np.pi * t / dur) ** 2 * 0.35


def timpani(f: float, dur: float = 1.2) -> np.ndarray:
    t = t_axis(dur)
    return (np.sin(2 * np.pi * f * t) + 0.3 * np.sin(2 * np.pi * f * 1.5 * t)) * np.exp(-t * 3.5) * env(len(t), 0.003, 0.0, 1, 0.1)


def place(track: np.ndarray, sound: np.ndarray, at: float, gain: float = 1.0) -> None:
    i = int(at * SR)
    if i >= len(track):
        return
    end = min(len(track), i + len(sound))
    track[i:end] += sound[: end - i] * gain


def reverb(x: np.ndarray, seconds: float = 1.4, mix: float = 0.18) -> np.ndarray:
    t = t_axis(seconds)
    ir = rng.uniform(-1, 1, len(t)) * np.exp(-t * 4.5)
    ir = lowpass(ir, 4_500)
    wet = np.convolve(x, ir)[: len(x)]
    wet /= np.max(np.abs(wet)) + 1e-9
    return x + mix * wet * np.max(np.abs(x))


def finish(x: np.ndarray, peak_db: float = -1.5) -> np.ndarray:
    x = np.tanh(x / (np.max(np.abs(x)) + 1e-9) * 1.15)
    return x / np.max(np.abs(x)) * 10 ** (peak_db / 20)


def loop(render, bars: int, bpm: float, beats_per_bar: int = 4) -> np.ndarray:
    """Renders [bars] plus a tail and folds the tail onto the start, so the loop is seamless."""
    length = bars * beats_per_bar * 60 / bpm
    track = np.zeros(int((length + 3) * SR))
    render(track, 60 / bpm)
    track = reverb(track)
    n = int(length * SR)
    body = track[:n].copy()
    body[: len(track) - n] += track[n:]
    return body


# ----------------------------------------------------------------------------- music

CHORDS_MENU = [["D3", "F3", "A3", "C4"], ["G3", "B3", "D4", "F4"], ["C3", "E3", "G3", "B3"], ["A2", "C#3", "E3", "G3"]]


def music_menu(track: np.ndarray, beat: float) -> None:
    """Lounge at the card table: electric piano ii-V-I-VI, walking bass, brushed hats. 92 bpm."""
    walk = [["D2", "F2", "A2", "C3"], ["G2", "B2", "D3", "F3"], ["C2", "E2", "G2", "B2"], ["A1", "C#2", "E2", "G2"]]
    for bar in range(16):
        start = bar * 4 * beat
        chord = CHORDS_MENU[bar % 4]
        for hit, length in ((0, 1.5), (1.5, 0.5), (2.5, 1.5)):
            for name in chord:
                place(track, epiano(note_hz(name), length * beat + 0.3), start + hit * beat, 0.16)
        for b, name in enumerate(walk[bar % 4]):
            place(track, bass(note_hz(name), beat * 0.95), start + b * beat, 0.42)
        for b in range(4):
            place(track, hat(), start + b * beat, 0.22)
            place(track, hat(), start + (b + 0.66) * beat, 0.14)
        if bar % 2 == 1:
            place(track, snare(0.15) * 0.4, start + 3 * beat, 0.25)


def music_football(track: np.ndarray, beat: float) -> None:
    """Stadium drive: kick on every beat, claps, chant-like synth stabs, I-V-vi-IV in E. 124 bpm."""
    chords = [["E3", "G#3", "B3"], ["B2", "D#3", "F#3"], ["C#3", "E3", "G#3"], ["A2", "C#3", "E3"]]
    roots = ["E2", "B1", "C#2", "A1"]
    for bar in range(16):
        start = bar * 4 * beat
        chord = chords[bar % 4]
        for b in range(4):
            place(track, kick(), start + b * beat, 0.9)
            place(track, hat(open_=True), start + (b + 0.5) * beat, 0.25)
            for e in (0, 0.5):
                place(track, bass(note_hz(roots[bar % 4]), beat * 0.45, 1_400), start + (b + e) * beat, 0.4)
        place(track, clap(), start + beat, 0.5)
        place(track, clap(), start + 3 * beat, 0.5)
        for hit in (0, 0.75, 1.5, 2.5, 3.25):
            place(track, pad([note_hz(n) for n in chord], beat * 0.5, 3_200), start + hit * beat, 0.5)
        if bar % 4 == 3:
            for k in range(4):
                place(track, snare(), start + (3 + k * 0.25) * beat, 0.35)


def music_basketball(track: np.ndarray, beat: float) -> None:
    """Arena funk: syncopated bass, claps on 2 and 4, Dm7-G7 vamp. 100 bpm."""
    vamp = [["D3", "F3", "A3", "C4"], ["G3", "B3", "D4", "F4"]]
    line = [("D2", 0), ("D2", 0.75), ("F2", 1.5), ("G2", 2), ("A2", 2.75), ("C3", 3.25), ("A2", 3.5)]
    for bar in range(16):
        start = bar * 4 * beat
        place(track, kick(), start, 0.9)
        place(track, kick(), start + 1.75 * beat, 0.7)
        place(track, kick(), start + 2.5 * beat, 0.8)
        place(track, clap(), start + beat, 0.55)
        place(track, clap(), start + 3 * beat, 0.55)
        for s in range(8):
            place(track, hat(), start + s * 0.5 * beat, 0.3 if s % 2 else 0.18)
        shift = 0 if bar % 2 == 0 else 5  # up a fourth on the G7 bar
        for name, at in line:
            f = note_hz(name) * 2 ** (shift / 12)
            place(track, bass(f, beat * 0.3, 1_800), start + at * beat, 0.5)
        chord = vamp[bar % 2]
        for hit in (0.5, 2.25):
            for name in chord:
                place(track, epiano(note_hz(name), beat * 0.4), start + hit * beat, 0.13)


def music_movies(track: np.ndarray, beat: float) -> None:
    """Cinema: string pad, timpani pulse, a slow horn-like line in A minor. 76 bpm."""
    chords = [["A2", "E3", "A3", "C4"], ["F2", "C3", "F3", "A3"], ["C3", "G3", "C4", "E4"], ["G2", "D3", "G3", "B3"]]
    melody = ["E4", "C4", "G4", "D4"]
    for bar in range(8):
        start = bar * 4 * beat
        chord = chords[bar % 4]
        place(track, pad([note_hz(n) for n in chord], 4 * beat + 0.5, 1_400), start, 0.6)
        place(track, timpani(note_hz(chord[0]) / 2), start, 0.6)
        place(track, timpani(note_hz(chord[0]) / 2), start + 2.5 * beat, 0.35)
        place(track, timpani(note_hz(chord[0]) / 2), start + 3 * beat, 0.45)
        t = t_axis(2 * beat)
        f = note_hz(melody[bar % 4])
        horn = lowpass(saw(f, t, 6), 1_100) * env(len(t), 0.15, 0.2, 0.7, 0.4)
        place(track, horn, start + 2 * beat, 0.35)


def music_food(track: np.ndarray, beat: float) -> None:
    """Kitchen bossa: nylon guitar, shaker, soft rim clicks, Cmaj7-Am7-Dm7-G7. 112 bpm."""
    chords = [["C3", "E3", "G3", "B3"], ["A2", "C3", "E3", "G3"], ["D3", "F3", "A3", "C4"], ["G2", "B2", "D3", "F3"]]
    comp = (0, 0.75, 1.5, 2.5, 3.25)
    for bar in range(16):
        start = bar * 4 * beat
        chord = chords[bar % 4]
        place(track, pluck(note_hz(chord[0]) / 2, 1.0, 0.3), start, 0.5)
        place(track, pluck(note_hz(chord[0]) * 0.75, 0.8, 0.3), start + 2 * beat, 0.45)
        for hit in comp:
            for k, name in enumerate(chord[1:]):
                place(track, pluck(note_hz(name), 0.5, 0.6), start + hit * beat + k * 0.008, 0.22)
        for s in range(8):
            place(track, shaker(), start + s * 0.5 * beat, 0.6 if s % 2 else 0.35)
        for hit in (0, 0.75, 1.75, 2.5, 3.5):
            place(track, hat(0.03), start + hit * beat, 0.25)


# ----------------------------------------------------------------------------- sound effects

def sfx_bid() -> np.ndarray:
    """A chip set on the felt: a short wooden clack."""
    t = t_axis(0.12)
    click = highpass(rng.uniform(-1, 1, len(t)), 2_500) * np.exp(-t * 90)
    knock = np.sin(2 * np.pi * 1_250 * t) * np.exp(-t * 60) + 0.5 * np.sin(2 * np.pi * 2_900 * t) * np.exp(-t * 80)
    return click * 0.5 + knock


def sfx_pass() -> np.ndarray:
    """A soft swipe."""
    t = t_axis(0.25)
    noise = rng.uniform(-1, 1, len(t))
    sweep = np.zeros_like(noise)
    for i, cut in enumerate(np.linspace(600, 3_500, 6)):
        part = slice(i * len(t) // 6, (i + 1) * len(t) // 6)
        sweep[part] = lowpass(noise, cut)[part]
    return sweep * np.sin(np.pi * t / t[-1])


def chime(notes: list[str], gap: float, length: float = 0.6) -> np.ndarray:
    out = np.zeros(int((gap * len(notes) + length) * SR))
    for i, name in enumerate(notes):
        t = t_axis(length)
        f = note_hz(name)
        tone = (np.sin(2 * np.pi * f * t) + 0.35 * np.sin(2 * np.pi * f * 2 * t) + 0.12 * np.sin(2 * np.pi * f * 3 * t)) * np.exp(-t * 6)
        place(out, tone * env(len(t), 0.003, 0, 1, 0.05), i * gap)
    return out


def sfx_tick() -> np.ndarray:
    t = t_axis(0.05)
    return np.sin(2 * np.pi * 1_900 * t) * np.exp(-t * 120)


def sfx_time_up() -> np.ndarray:
    """A short buzzer."""
    t = t_axis(0.45)
    x = np.sign(np.sin(2 * np.pi * 220 * t)) * 0.5 + np.sign(np.sin(2 * np.pi * 226 * t)) * 0.5
    return lowpass(x, 2_200) * env(len(t), 0.005, 0.05, 0.8, 0.08)


def sfx_win() -> np.ndarray:
    return reverb(chime(["C5", "E5", "G5", "C6"], 0.09, 0.9), 1.0, 0.25)


def sfx_lose() -> np.ndarray:
    return reverb(chime(["G4", "E4", "C4"], 0.16, 0.9), 1.0, 0.2)


def sfx_draw() -> np.ndarray:
    return reverb(chime(["C5", "G4", "C5"], 0.14, 0.8), 1.0, 0.2)


# ----------------------------------------------------------------------------- output

def write_mp3(name: str, x: np.ndarray, bitrate: str) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / f"{name}.wav"
        wavfile.write(wav, SR, (x * 32_767).astype(np.int16))
        subprocess.run(
            ["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-ac", "1", "-codec:a", "libmp3lame", "-b:a", bitrate, str(OUT / f"{name}.mp3")],
            check=True,
        )
    print(f"{name}.mp3  {len(x) / SR:5.1f} s  {(OUT / f'{name}.mp3').stat().st_size // 1024} KB")


def main() -> int:
    music = {
        "music_menu": (music_menu, 16, 92),
    }
    for name, (render, bars, bpm) in music.items():
        write_mp3(name, finish(loop(render, bars, bpm), -3.0), "96k")
    sfx = {
        "sfx_bid": sfx_bid(), "sfx_pass": sfx_pass(), "sfx_card_won": chime(["E5", "B5"], 0.07, 0.5),
        "sfx_card_lost": chime(["D5", "A4"], 0.08, 0.45), "sfx_tick": sfx_tick(), "sfx_time_up": sfx_time_up(),
        "sfx_win": sfx_win(), "sfx_lose": sfx_lose(), "sfx_draw": sfx_draw(),
    }
    for name, x in sfx.items():
        write_mp3(name, finish(x, -2.0), "64k")
    return 0


if __name__ == "__main__":
    sys.exit(main())
