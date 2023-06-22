###
<img align="left" width="140" src="androidApp/src/main/ic_launcher-playstore.png">

# SpMp
A YouTube Music client with a focus on language and metadata customisation, built for Android using Jetpack Compose and Kotlin.

<br>

## Development status
SpMp is in alpha and still has many bugs, but is very close to being feature-complete. I've been using it instead of YouTube Music for a while now.

Support for Compose Multiplatform on desktop is [planned](https://github.com/toasterofbread/spmp-server) but will not be a priority until the main project is stable.

<br>

<img align="right" width="20%" src="readme/screenshot_2.png">

## Key features

#### Metadata
- Edit song, artist, and playlist titles
- Set separate languages for app UI and metadata like song titles

#### QOL
- Pin any song, playlist, album, or artist to the top of the main page
- Download songs for offline playback
- Select multiple songs/artists/playlists from any screen to perform general or context-specific actions (ex. shuffle just part of the queue)
- When adding a song to the queue from the long press menu, a specific position can be selected and songs are placed in the order you add them by default

#### Connectivity
- In-app YouTube Music login for feed personalisation and interaction
- Customisable Discord rich presence (with image support) using [KizzyRPC](https://github.com/dead8309/KizzyRPC)

#### Lyrics
- Display lyrics from [PetitLyrics](https://petitlyrics.com/), with timed lyrics support
- Timed lyrics are displayed in a toggleable bar above every UI page in the app
- Show furigana for Japanese kanji within lyrics using [Kuromoji](https://github.com/atilika/kuromoji)

##### And more (see [the wiki](https://github.com/toasterofbread/spmp/wiki) for a ~~full~~ more complete list of features)

## Planned features
- Full offline functionality
- ...?

<details open>
    <summary><h2>Screenshots</h2></summary>
    <p align="center">
        <img src="readme/screenshot_11.png" style="max-height:70vh;object-fit:contain;" width="30%">
        <img src="readme/screenshot_5.png" style="max-height:70vh;object-fit:contain;" width="30%">
        <img src="readme/screenshot_10.png" style="max-height:70vh;object-fit:contain;" width="30%">
    </p>
</details>

<details closed>
    <summary><h2>More Screenshots</h2></summary>
    <p align="center">
        <img src="readme/screenshot_15.png" style="max-height:70vh;object-fit:contain;" width="30%">
        <img src="readme/screenshot_19.png" style="max-height:70vh;object-fit:contain;" width="30%">
        <img src="readme/screenshot_16.png" style="max-height:70vh;object-fit:contain;" width="30%">
    </p>
    <p align="center">
        <img src="readme/screenshot_14.png" style="max-height:70vh;object-fit:contain;" width="30%">
        <img src="readme/screenshot_17.png" style="max-height:70vh;object-fit:contain;" width="30%">
        <img src="readme/screenshot_8.png" style="max-height:70vh;object-fit:contain;" width="30%">
    </p>
</details>

<br>

## About the project
I started this project after I got tired of dealing with YouTube's official music app's lack of language features and customisation. I tried several alternatives, but all had at least a few issues that bugged me.

So instead of spending a few weeks learning the codebase of an existing project and contributing to it, I decided to spend almost a year (as of writing) creating my own solution.

## Contributing
Pull requests and feature suggestions are welcome! This is my first Compose project (and also the largest project I've ever done in general) so there's probably plenty of room for improvement.

The app is currently being developed for both English and Japanese. Contributions for other languages would be greatly appreciated!

## Thanks to
- [ytmusicapi](https://github.com/sigma67/ytmusicapi/): Used as a reference for the YouTube Music API
- [KeyMapper](https://github.com/keymapperorg/KeyMapper): For screen off volume control implementation
- [ExoVisualizer](https://github.com/dzolnai/ExoVisualizer): Music visualiser implementation
- [ViMusic](https://github.com/vfsfitvnm/ViMusic): A major inspiration for this project

## Disclaimer
This project and its contents are not affiliated with, funded, authorized, endorsed by, or in any way associated with YouTube, Google LLC or any of its affiliates and subsidiaries.

Any trademark, service mark, trade name, or other intellectual property rights used in this project are owned by the respective owners.
