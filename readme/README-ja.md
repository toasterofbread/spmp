[英語のREADMEへ](../README.md)

###
<img align="left" width="140" src="../androidApp/src/main/ic_launcher-playstore.png">

# SpMp
言語とメタデータのカスタマイズに特化した YouTube Music のアプリ。Jetpack ComposeとKotlinを使って主にAndroid向けに開発されています。

<br>

## 開発状態
SpMpはまだアルファ版であり、バグはまだ多く残っていますが、まもなく機能をすべて完成させてベータ版となる予定です。僕はすでにSpMpをYouTube Musicのかわりに使っています。

Compose Multiplatformでのデスクトップ上サポートは[計画](https://github.com/toasterofbread/spmp-server)されていますが、本プロジェクトが完成するまでは優先されません。

<br>

<img align="right" width="20%" src="screenshot_2.png">

## 基本の機能

#### メタデータ
- 曲、アーティスト、またはプレイリストのタイトルを編集
- UIとメタデータに別々の言語を設定

#### QOL
- 曲、プレイリスト、アーティストなどをどれでもトップページの上に貼り付け可能
- オフライン時再生用に曲をダウンロード
- どの画面からでも曲やプレイリストなどを一度に複数選択し、一般的なアクションまたはその画面用のアクションが使えます。（例：曲キューの一部のみをシャッフルまたは削除する）
- 長押しメニューから曲をキューに追加する時、具体的な位置を選んでから追加することができます。位置を選択をしなかれば、曲は逆の順番ではなく足された順に追加されます。

#### オンライン
- アプリ内でYouTube Musicにログインし、自分のフィードを表示したりアカウントでアーティストに登録することが可能。
- [KizzyRPC](https://github.com/dead8309/KizzyRPC)によるDiscordのアクティビティステータス

#### 歌詞
- [プチリリ](https://petitlyrics.com/ja/)からの歌詞を表示。（同期可能）
- 同期可能の歌詞はアプリすべての画面の上に表示することができます。
- [Kuromoji](https://github.com/atilika/kuromoji)を使って歌詞内の漢字の上にふりがなを表示

##### 他にもあります（[wiki](https://github.com/toasterofbread/spmp/wiki) をご覧ください)

## 計画されてる機能
- 完全オフライン機能性
- ...?

<details open>
    <summary><h2>スクリーンショット</h2></summary>
    <p align="center">
        <img src="screenshot_11.png" style="max-height:70vh;object-fit:contain;" width="30%">
        <img src="screenshot_5.png" style="max-height:70vh;object-fit:contain;" width="30%">
        <img src="screenshot_10.png" style="max-height:70vh;object-fit:contain;" width="30%">
    </p>
</details>

<details closed>
    <summary><h2>他のスクリーンショット</h2></summary>
    <p align="center">
        <img src="screenshot_15.png" style="max-height:70vh;object-fit:contain;" width="30%">
        <img src="screenshot_19.png" style="max-height:70vh;object-fit:contain;" width="30%">
        <img src="screenshot_16.png" style="max-height:70vh;object-fit:contain;" width="30%">
    </p>
    <p align="center">
        <img src="screenshot_14.png" style="max-height:70vh;object-fit:contain;" width="30%">
        <img src="screenshot_17.png" style="max-height:70vh;object-fit:contain;" width="30%">
        <img src="screenshot_8.png" style="max-height:70vh;object-fit:contain;" width="30%">
    </p>
</details>

<br>

## このプロジェクトについて

YouTube公式のミュージックアプリをしばらく使っていましたが、言語やメタデータの設定機能の無さを感じて別のアプリをいくつか使ってみました。ましな物はあったけど、どれにも重大な問題を一つは感じました。

という訳で、数週間かけて使ってみたアプリのどれかのレポジトリーに参加するかわりに、ほぼ一年かけて自分でアプリを作ることにしました。

## コントリビュートする
Pull requestと機能の提案は歓迎です！これは僕の初めてのComposeプロジェクトで、それに今までで最大のプロジェクトなので改善の余地はたくさんあると思います。

このアプリの日本語版も英語版も開発しています。他の言語への貢献も大歓迎です！

## 参考にしたソフト
- [ytmusicapi](https://github.com/sigma67/ytmusicapi/): YouTube Music APIの使い方の参考にさせてもらいました
- [KeyMapper](https://github.com/keymapperorg/KeyMapper): 画面オフ時の音量調整実装の参考にさせてもらいました
- [ExoVisualizer](https://github.com/dzolnai/ExoVisualizer): 音楽ビジュアライザの実装の参考にさせてもらいました
- [ViMusic](https://github.com/vfsfitvnm/ViMusic): このプロジェクトへの大きなインスピレーションでした

## 免責事項
このプロジェクトおよびその内容は、YouTube、Google LLC、またはそれらの関連会社といかなる関連性も持っておらず、それらによって承認されたものではありません。

このプロジェクトで使用されている商標、サービスマーク、商号、その他の知的財産権は、それぞれの所有者に帰属しています。
