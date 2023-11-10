<p>
    <a href="../README.md">To the English README</a>
    <a href="https://discord.gg/B4uY4FkkJ3"><img src="https://img.shields.io/discord/1133321339495788625?style=for-the-badge&logo=discord&label=Discord&color=4f58d6" align="right"></a>
</p>

<br>

###
<img align="left" width="140" src="../androidApp/src/main/ic_launcher-playstore.png">

<a href="https://github.com/toasterofbread/spmp/blob/main/LICENSE"><img src="https://img.shields.io/github/license/toasterofbread/spmp?style=for-the-badge&color=624c9a&label=%E3%83%A9%E3%82%A4%E3%82%BB%E3%83%B3%E3%82%B9" align="right"></a>
<a href="https://github.com/toasterofbread/spmp/commits/main"><img src="https://img.shields.io/github/commits-since/toasterofbread/spmp/latest?style=for-the-badge&color=624c9a&label=%E6%9C%80%E6%96%B0%E3%81%AE%E3%83%AA%E3%83%AA%E3%83%BC%E3%82%B9%E3%81%8B%E3%82%89%E3%81%AE%E3%82%B3%E3%83%9F%E3%83%83%E3%83%88" align="right"></a>
<a href="https://github.com/toasterofbread/spmp/releases"><img src="https://img.shields.io/github/v/release/toasterofbread/spmp?logo=github&style=for-the-badge&color=624c9a&label=%E3%83%AA%E3%83%AA%E3%83%BC%E3%82%B9" align="right"></a>

# SpMp
言語とメタデータのカスタマイズに特化した YouTube Music のアプリ。Jetpack ComposeとKotlinを使って主にAndroid向けに開発されています。

<br>
<br>

## 開発状態
SpMpはまだアルファ版であり、バグはまだ多く残っていますが、まもなく機能をすべて完成させてベータ版となる予定です。僕はすでにSpMpをYouTube Musicのかわりに使っています。

Compose Multiplatformでの[デスクトップ上サポート](https://github.com/toasterofbread/spmp-server)は[開発中](https://github.com/toasterofbread/spmp/pull/170)ですが、本プロジェクトが完成するまでは優先されません。

<br>

<img align="right" width="20%" src="../metadata/en-US/images/phoneScreenshots/0.png">

## 機能
- 曲、アーティスト、またはプレイリストのタイトルを編集
- UIとメタデータに別々の言語を設定
- アプリ内でYouTube Musicにログイン
- [KuGou](https://www.kugou.com/)と[プチリリ](https://petitlyrics.com/ja/)から同期可能の歌詞を表示
    - 同期可能の歌詞をアプリ内すべての画面の上に表示
    - 歌詞内の漢字の上にふりがなを表示
- どの画面からでも曲を複数選択
- 曲、アーティスト、プレイリストをメイン画面に貼り付ける
- 曲をキューに追加する時、簡単に位置を選択
- Discordリッチプレゼンス

##### 他にもあります ([wiki](https://github.com/toasterofbread/spmp/wiki) をご覧ください)

<details open>
    <summary><h2>スクリーンショット</h2></summary>
    <p align="center">
        <img src="../metadata/en-US/images/phoneScreenshots/7.png" style="max-height:70vh;object-fit:contain;" width="25%">
        <img src="../metadata/en-US/images/phoneScreenshots/8.png" style="max-height:70vh;object-fit:contain;" width="25%">
        <img src="../metadata/en-US/images/phoneScreenshots/1.png" style="max-height:70vh;object-fit:contain;" width="25%">
    </p>
</details>

<details closed>
    <summary><h2>他のスクリーンショット</h2></summary>
    <p align="center">
        <img src="../metadata/en-US/images/phoneScreenshots/9.png" style="max-height:70vh;object-fit:contain;" width="25%">
        <img src="../metadata/en-US/images/phoneScreenshots/2.png" style="max-height:70vh;object-fit:contain;" width="25%">
        <img src="../metadata/en-US/images/phoneScreenshots/4.png" style="max-height:70vh;object-fit:contain;" width="25%">
    </p>
    <p align="center">
        <img src="../metadata/en-US/images/phoneScreenshots/3.png" style="max-height:70vh;object-fit:contain;" width="25%">
        <img src="../metadata/en-US/images/phoneScreenshots/5.png" style="max-height:70vh;object-fit:contain;" width="25%">
        <img src="../metadata/en-US/images/phoneScreenshots/6.png" style="max-height:70vh;object-fit:contain;" width="25%">
    </p>
</details>

<br>

## このプロジェクトについて
YouTube公式のミュージックアプリをしばらく使っていましたが、言語やメタデータの設定機能の無さを感じて別のアプリをいくつか使ってみました。ましな物はあったけど、どれにも重大な問題を一つは感じました。

という訳で、数週間かけて使ってみたアプリのどれかのレポジトリーに参加するかわりに、ほぼ一年かけて自分でアプリを作ることにしました。

## コントリビュートする
Pull requestと機能の提案は歓迎です！これは僕の初めてのComposeプロジェクトで、それに今までで最大のプロジェクトなので改善の余地はたくさんあると思います。

このアプリの日本語版も英語版も開発しています。他の言語への貢献も大歓迎です！

## こちらからも入手

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="100">](https://f-droid.org/en/packages/com.toasterofbread.spmp/)

## 参考にしたソフト
- [ytmusicapi](https://github.com/sigma67/ytmusicapi/): YouTube Music APIの使い方の参考にさせてもらいました
- [KeyMapper](https://github.com/keymapperorg/KeyMapper): 画面オフ時の音量調整実装の参考にさせてもらいました
- [ExoVisualizer](https://github.com/dzolnai/ExoVisualizer): 音楽ビジュアライザの実装の参考にさせてもらいました
- [ViMusic](https://github.com/vfsfitvnm/ViMusic): このプロジェクトへの大きなインスピレーションでした

#### ライブラリ（すべてのライブラリを[shared/build.gradle.kts](/shared/build.gradle.kts)確認できます）
- [Piped](https://github.com/TeamPiped/Piped): 曲再生用のストリームURLを提供
- [SQLDelight](https://github.com/cashapp/sqldelight): メディアのメタデータ用データベース
- [Kuromoji](https://github.com/atilika/kuromoji): 日本語歌詞のふりがなを生成
- [KizzyRPC](https://github.com/dead8309/KizzyRPC) and [Kord](https://github.com/kordlib/kord): Discordステータスとその画像機能
- [ComposeReorderable](https://github.com/aclassen/ComposeReorderable): 曲キューなどの順序変更可能なリスト
- [compose-color-picker](https://github.com/godaddy/compose-color-picker): テーマエディター内のカラーセレクター
- [Catppuccin](https://github.com/catppuccin/java): テーマがアプリのにオプションに含まれています

## 免責事項
このプロジェクトおよびその内容は、YouTube、Google LLC、またはそれらの関連会社といかなる関連性も持っておらず、それらによって承認されたものではありません。

このプロジェクトで使用されている商標、サービスマーク、商号、その他の知的財産権は、それぞれの所有者に帰属しています。
