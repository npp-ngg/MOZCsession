オープンソース[ＭＯＺＣ](https://github.com/google/mozc)のかな漢字変換部分を抜き出して自作のＧＵＩと組み合わせて使えるようにしました。  
ＭＯＺＣでは入力部分とかな漢字変換部分はサーバー・クライアント型でサーバーはＣ＋＋、Android版ではクライアントはＪａｖａで作られています。サーバー側の機能は共用ライブラリlibmozc.soが提供します。libmozc.soは本プロジェクトではビルドしません。別途ビルドしたものを使用します。  
本プロジェクトではＭＯＺＣから切り出したクライアント側のインターフェイス部分とその使い方を紹介しています。  
### 1. Android Studioプロジェクト  
------------  

- プロジェクトはＭＯＺＣ変換ライブラリとテスト用ＧＵＩ部分とに分れています
- ビルドするとかな漢字変換ＧＵＩインターフェイス部分とテスト用アプリが出来上がります
- mozc-client以下がＭＯＺＣ変換ライブラリ（ＧＵＩインターフェイス）、app以下がテスト用のＧＵＩです  
- SessionExecutorが分水嶺で変換機能はSessionExecutorオブジェクトを使って実装しＧＵＩ部分はSessionExecutorを呼び出して漢字変換を実現します  
- SessionExecutorクラスがビルド出来るように必要なモジュールを揃えていますが、出来るだけソースコードは変更しない方針で、コンパイルを通すため不要なキーボード関連のコードやリスースが残っています  
- ユーザ辞書登録などユティリティ関連は含んでいません
- libmozc.soはビルドしたものを[app/src/main/jniLibs](https://github.com/npp-ngg/MOZCsession/tree/master/app/src/main/jniLibs)に置いてあります
### 2. ＭＯＺＣソースコードについて  
---  
- AndroidのＧＵＩ部分と変換エンジンが揃った最後のバージョン[2018-02-26](https://github.com/google/mozc/tree/2018-02-26)です  
- 新しいバージョンはjni(mozcjni.cc)で互換性がありません
- かな漢字変換に必要な[session](https://github.com/google/mozc/tree/2018-02-26/src/android/src/com/google/android/inputmethod/japanese/session)を取り出して単体でビルド出来るように必要なモジュールを追加してあります  
- libmozc.soは[この手順](https://github.com/google/mozc/blob/2018-02-26/docs/build_mozc_in_docker.md)や[このスクリプト](https://github.com/google/mozc/blob/2018-02-26/docker/ubuntu14.04/Dockerfile)に従ってビルドしてください  
- [protbuf](https://github.com/npp-ngg/MOZCsession/tree/master/mozc-client/src/main/java/org/mozc/android/inputmethod/japanese/protobuf)下のソースコード、
[EmojiData.java](https://github.com/npp-ngg/MOZCsession/blob/master/mozc-client/src/main/java/org/mozc/android/inputmethod/japanese/emoji/EmojiData.java)はlibmozcのビルド時に生成されたものです  
### 3. アプリについて  
SessionExecutorを使ってかな漢字変換、候補リストの処理、ユーザ辞書の操作例です。Inputmethodを作成するための実装を試したものです。と言っても例にもならない位に簡略化しています。実際はＭＯＺＣの[MozcBaseService.java](https://github.com/google/mozc/blob/2018-02-26/src/android/src/com/google/android/inputmethod/japanese/MozcBaseService.java)を見てください。  
### 4. Mozcのライセンスについて  
***
mozc-client以下のソースコードはMozcのライセンスに従って公開します。  
> All Mozc code written by Google is released under The [BSD 3-Clause License](https://opensource.org/license/BSD-3-Clause).  
> Copyright 2010-2018, Google Inc.  
>  All rights reserved.
>  
> Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
> 
>  * Redistributions of source code must retain the above copyright  
>    notice, this list of conditions and the following disclaimer.  
>  * Redistributions in binary form must reproduce the above  
>    copyright notice, this list of conditions and the following disclaimer  
>    in the documentation and/or other materials provided with the  
>    distribution.  
>  * Neither the name of Google Inc. nor the names of its  
>    contributors may be used to endorse or promote products derived from  
>    this software without specific prior written permission.  
>  
> THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS  
> "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT  
> LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR  
> A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT  
> OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,  
> SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT  
> LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,  
> DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY  
> THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT  
> (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE  
> OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.  
