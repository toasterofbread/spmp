<p>
    <a href="../README.md">To the English README</a>
    <a href="https://discord.gg/B4uY4FkkJ3"><img src="https://img.shields.io/discord/1133321339495788625?style=for-the-badge&logo=discord&label=Discord&color=4f58d6" align="right"></a>
</p>

<br>

###
<img align="left" width="140" src="../metadata/en-US/images/icon_round.png">

<a href="https://github.com/toasterofbread/spmp/blob/main/LICENSE"><img src="https://img.shields.io/github/license/toasterofbread/spmp?style=for-the-badge&color=624c9a&label=%E3%83%A9%E3%82%A4%E3%82%BB%E3%83%B3%E3%82%B9" align="right"></a>
<a href="https://github.com/toasterofbread/spmp/releases"><img src="https://img.shields.io/github/v/release/toasterofbread/spmp?logo=github&style=for-the-badge&color=624c9a&label=%E3%83%AA%E3%83%AA%E3%83%BC%E3%82%B9" align="right"></a>

# SpMp
UIの色と曲のメタデータのカスタマイズに特化した YouTube Music のアプリ。Androidとデスクトップのプラットフォーム向けに、Compose Multiplatformを使って開発されています。

<br>

## 機能
- 曲、アーティスト、またはプレイリストのタイトルを編集
- どの画面からでも曲を複数選択
- アプリ全体のテーマのカスタマイズが可能
    - 自動的に曲のイメージからの色を使う
    - もしくはユーザーに選択された色を使用
- UIとメタデータに別々の言語を設定
- アプリ内でYouTube Musicにログイン
- [KuGou](https://www.kugou.com/)と[プチリリ](https://petitlyrics.com/ja/)から同期可能の歌詞を表示
    - 同期可能の歌詞をアプリ内すべての画面の上に表示
    - 歌詞内の漢字の上にふりがなを表示
- 曲、アーティスト、プレイリストをメイン画面に貼り付ける
- Discordリッチプレゼンス
- 曲をキューに追加する時、簡単に位置を選択

##### 他にもあります ([wiki](https://github.com/toasterofbread/spmp/wiki) をご覧ください)

<details open>
    <summary><h2>スクリーンショット</h2></summary>
    <p align="center">
        <img src="../metadata/en-US/images/phoneScreenshots/landscape_1.png" style="max-height:50vh;object-fit:contain;" width="78%">
        <img width="21%" src="../metadata/en-US/images/phoneScreenshots/8.png" style="max-height:50vh;object-fit:contain;">
    </p>
    <p align="center">
        <img width="21%" src="../metadata/en-US/images/phoneScreenshots/1.png" style="max-height:50vh;object-fit:contain;">
        <img src="../metadata/en-US/images/phoneScreenshots/landscape_2.png" style="max-height:50vh;object-fit:contain;" width="78%">
    </p>
</details>

<details closed>
    <summary><h2>他のスクリーンショット</h2></summary>
    <p align="center">
        <img src="../metadata/en-US/images/phoneScreenshots/9.png" style="max-height:70vh;object-fit:contain;" width="25%">
        <img src="../metadata/en-US/images/phoneScreenshots/7.png" style="max-height:70vh;object-fit:contain;" width="25%">
        <img src="../metadata/en-US/images/phoneScreenshots/4.png" style="max-height:70vh;object-fit:contain;" width="25%">
    </p>
    <p align="center">
        <img src="../metadata/en-US/images/phoneScreenshots/0.png" style="max-height:70vh;object-fit:contain;" width="25%">
        <img src="../metadata/en-US/images/phoneScreenshots/5.png" style="max-height:70vh;object-fit:contain;" width="25%">
        <img src="../metadata/en-US/images/phoneScreenshots/6.png" style="max-height:70vh;object-fit:contain;" width="25%">
    </p>
</details>

<br>

## インストール手順

すべてのプラットフォームのダウンロードは、リポジトリの[リリース ページ](https://github.com/toasterofbread/spmp/releases)にあります。

#### デスクトップ版の追加要件

- Java をインストールする（すべてのプラットフォーム）
- Linux バージョンには[いくつかのシステム パッケージ](https://spmp.toastbits.dev/docs/latest/client/installation/#dependency)が必要です

Android版は F-Droid からもダウンロードできます。GitHubで入手可能の APK と F-Droid で入手可能の APK に違いはありません。

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="80">](https://f-droid.org/en/packages/com.toasterofbread.spmp/)

## ドキュメンテーション（現在英語のみ）

SpMp の高度な使用法（コンパイル手順など）に関するドキュメンテーションは、https://spmp.toastbits.dev/docs/latest/ にあります。

## このプロジェクトについて
YouTube公式のミュージックアプリをしばらく使っていましたが、言語やメタデータの設定機能の無さを感じて別のアプリをいくつか使ってみました。ましな物はあったけど、どれにも重大な問題を一つは感じました。

という訳で、数週間かけて使ってみたアプリのどれかのレポジトリーに参加するかわりに、ほぼ一年かけて自分でアプリを作ることにしました。

## コントリビュートする
Pull requestと機能の提案は歓迎です！これは僕の初めてのComposeプロジェクトで、それに今までで最大のプロジェクトなので改善の余地はたくさんあると思います。

このアプリの日本語版も英語版も開発しています。他の言語への貢献も大歓迎です！

## 参考にしたソフト
- smlqrs: 本プロジェクトのアイコンをデザインしてくれました
- [ytmusicapi](https://github.com/sigma67/ytmusicapi/): YouTube Music APIの使い方の参考にさせてもらいました
- [ExoVisualizer](https://github.com/dzolnai/ExoVisualizer): 音楽ビジュアライザの実装の参考にさせてもらいました
- [ViMusic](https://github.com/vfsfitvnm/ViMusic): このプロジェクトへの大きなインスピレーションでした

#### ライブラリ（すべてのライブラリを[shared/build.gradle.kts](/shared/build.gradle.kts)確認できます）
- [Piped](https://github.com/TeamPiped/Piped): 曲再生用のストリームURLを提供
- [SQLDelight](https://github.com/cashapp/sqldelight): メディアのメタデータ用データベース
- [Kuromoji](https://github.com/atilika/kuromoji): 日本語歌詞のふりがなを生成
- [KizzyRPC](https://github.com/dead8309/KizzyRPC): Discordステータス機能
- [ComposeReorderable](https://github.com/aclassen/ComposeReorderable): 曲キューなどの順序変更可能なリスト
- [compose-color-picker](https://github.com/godaddy/compose-color-picker): テーマエディター内のカラーセレクター
- [Catppuccin](https://github.com/catppuccin/java): テーマがアプリのにオプションに含まれています

## 免責事項
このプロジェクトおよびその内容は、YouTube、Google LLC、またはそれらの関連会社といかなる関連性も持っておらず、それらによって承認されたものではありません。

このプロジェクトで使用されている商標、サービスマーク、商号、その他の知的財産権は、それぞれの所有者に帰属しています。
