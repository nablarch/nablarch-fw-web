# nablarch-fw-web 

## テストの実行方法

本モジュールのテストは、実行時の Java のバージョンに合わせて Maven のプロファイルを指定する必要がある。

|Java|プロファイル|
|----|------------|
|6   |-           |
|8   |`java8`     |
|9   |`java9`     |
|11  |`java11`    |

例

```sh
# Java 6 で実行する場合
$ mvn test

# Java 8 で実行する場合
$ mvn -P java8 test

# Java 11 で実行する場合
$ mvn -P java11 test
```

## Jetty 6, 9 用のテストコードの管理

本モジュールは Java 6 以上を前提としている。このため、デフォルトでは Jetty 6 を使ったテストを実行するように構成されている。
Jetty 6 は Servlet API の 2.5 を使用するため、テスト用のモッククラスは 2.5 の `HttpServletRequest` などを実装している。

しかし、 Java 11 でテストを実行する場合は Jetty を 9 に上げる必要がある。
Jetty 9 では、 Servlet API が 3.1 に上がり、一部のモッククラスは追加されたメソッドをオーバーライドしなければならない。
このため、 Jetty 6 用のモッククラスと Jetty 9 用のモッククラスは互換性を保つことができない。

そこで、 Jetty 6 用と Jetty 9 用のテストコードは、それぞれ以下の場所に分けて管理している。

- Jetty 6 用
  - `src/test/jetty6/java`
- Jetty 9 用
  - `src/test/jetty9/java`

これらのソースコードは、 Maven のプロファイルを使ってテストコードとして使用するかどうかを切り替えている。
デフォルトでは `src/test/jetty6/java` の下のソースコードが使用される。
そして、 `java11` プロファイルを有効にした場合は、 `src/test/jetty9/java` の下のソースコードが使用されるようになっている。

IntelliJ で開発する場合、 `java11` プロファイルを有効にして Maven プロジェクトのリロードを行えば、 `src/test/jetty9/java` をソースディレクトリとして認識させられるようになる。
ただし、その場合は `src/test/jetty6/java` をソースディレクトリから外す必要がある（クラスが重複するため）。
ソースディレクトリから外すには、手動で Module Settings から当該ディレクトリを Unmark する。
