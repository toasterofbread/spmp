<p>
    <a href="readme/README-ja.md">日本語のREADMEへ</a>
    <a href="https://discord.gg/B4uY4FkkJ3"><img src="https://img.shields.io/discord/1133321339495788625?style=for-the-badge&logo=discord&label=Discord&color=4f58d6" align="right"></a>
</p>

<br>

###
<img align="left" width="140" src="metadata/en-US/images/icon_round.png">

<a href="https://github.com/toasterofbread/spmp/blob/main/LICENSE"><img src="https://img.shields.io/github/license/toasterofbread/spmp?style=for-the-badge&color=624c9a" align="right"></a>
<a href="https://github.com/toasterofbread/spmp/releases"><img src="https://img.shields.io/github/v/release/toasterofbread/spmp?logo=github&style=for-the-badge&color=624c9a" align="right"></a>

# SpMp
A YouTube Music client with a focus on customisation of colours and song metadata, built using Compose Multiplatform for Android and desktop

<br>
<br>

## Development status
The Android version of SpMp is in beta and is ready for regular use with no major issues, but rough edges are still being actively polished and tweaked.

SpMp is being [ported](https://github.com/toasterofbread/spmp-server) to desktop platforms of [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform), but is still in a pre-alpha state. Some basic features are working but are yet to be tested thoroughly.

<br>

<img align="right" width="20%" src="metadata/en-US/images/phoneScreenshots/0.png">

# Features
- Edit song, artist, and playlist titles
- Select multiple songs for batch actions on any screen
- Customisable colour theming throughout the app
    - Use a colour from the current song's thumbnail
    - Or use a custom colour set per-song or globally
- Display app UI and metadata like song titles in separate languages
- In-app YouTube Music login
- Display time-synchronised lyrics from [KuGou](https://www.kugou.com/) and [PetitLyrics](https://petitlyrics.com/)
    - Timed lyrics are displayed in a toggleable bar above every app page
    - Furigana (readings) display above Japanese kanji within lyrics
- Pin any song, playlist, album, or artist to the top of the main page
- Customisable Discord rich presence
- Easily insert songs at any position in the queue

##### And more (see [the wiki](https://github.com/toasterofbread/spmp/wiki) for a ~~full~~ more complete list of features)

<details open>
    <summary><h2>Screenshots</h2></summary>
    <p align="center">
        <img src="metadata/en-US/images/phoneScreenshots/7.png" style="max-height:70vh;object-fit:contain;" width="25%">
        <img src="metadata/en-US/images/phoneScreenshots/8.png" style="max-height:70vh;object-fit:contain;" width="25%">
        <img src="metadata/en-US/images/phoneScreenshots/1.png" style="max-height:70vh;object-fit:contain;" width="25%">
    </p>
</details>

<details closed>
    <summary><h2>More Screenshots</h2></summary>
    <p align="center">
        <img src="metadata/en-US/images/phoneScreenshots/9.png" style="max-height:70vh;object-fit:contain;" width="25%">
        <img src="metadata/en-US/images/phoneScreenshots/2.png" style="max-height:70vh;object-fit:contain;" width="25%">
        <img src="metadata/en-US/images/phoneScreenshots/4.png" style="max-height:70vh;object-fit:contain;" width="25%">
    </p>
    <p align="center">
        <img src="metadata/en-US/images/phoneScreenshots/3.png" style="max-height:70vh;object-fit:contain;" width="25%">
        <img src="metadata/en-US/images/phoneScreenshots/5.png" style="max-height:70vh;object-fit:contain;" width="25%">
        <img src="metadata/en-US/images/phoneScreenshots/6.png" style="max-height:70vh;object-fit:contain;" width="25%">
    </p>
</details>

<br>

## About the project
I started this project after I got tired of dealing with YouTube's official music app's lack of language features and customisation. I tried several alternatives, but all had at least a few issues that bugged me.

So instead of spending a few weeks learning the codebase of an existing project and contributing to it, I decided to spend almost a year (as of writing) creating my own solution.

## Contributing
Pull requests, bug reports, and feature suggestions are welcome! This is my first Compose project (and also the largest project I've ever done in general) so there's probably plenty of room for improvement.

The app is currently being developed for both English and Japanese. Contributions for other languages would be greatly appreciated! Information about app localisation can be found [here](https://github.com/toasterofbread/spmp/wiki/App-localisation).

## Also available at

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="100">](https://f-droid.org/en/packages/com.toasterofbread.spmp/)

## Thanks to
- smlqrs: For designing the project's icon graphic
- [ytmusicapi](https://github.com/sigma67/ytmusicapi/): Used as a reference for the YouTube Music API
- [ExoVisualizer](https://github.com/dzolnai/ExoVisualizer): Music visualiser implementation
- [ViMusic](https://github.com/vfsfitvnm/ViMusic): A major inspiration for this project

#### Libraries (see all in [shared/build.gradle.kts](/shared/build.gradle.kts))
- [Piped](https://github.com/TeamPiped/Piped): Provides stream URLs for music playback
- [SQLDelight](https://github.com/cashapp/sqldelight): Database for media metadata
- [Kuromoji](https://github.com/atilika/kuromoji): Generates furigana (readings) for Japanese lyrics
- [KizzyRPC](https://github.com/dead8309/KizzyRPC): Discord status functionality
- [ComposeReorderable](https://github.com/aclassen/ComposeReorderable): Reorderable lists like the song queue
- [compose-color-picker](https://github.com/godaddy/compose-color-picker): Theme editor colour wheel
- [Catppuccin](https://github.com/catppuccin/java): Themes included as options in-app

## Disclaimer
This project and its contents are not affiliated with, funded, authorized, endorsed by, or in any way associated with YouTube, Google LLC or any of its affiliates and subsidiaries.

Any trademark, service mark, trade name, or other intellectual property rights used in this project are owned by the respective owners.
