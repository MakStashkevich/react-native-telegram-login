# React Native Telegram Login

### ЭТО ЭКСПЕРИМЕНТАЛЬНЫЙ ПРОЕКТ, КОТОРЫЙ НЕ СРАБОТАЛ! 😭

<p align="center">
  <img src="readme-assets/screenshot.jpg" alt="Скриншот работы React Native Telegram Авторизации" width="400"/>
</p>

## Как я делал вход через Telegram и почему он не работает..

На одном из проектов я делаю React Native приложение для Android и IOS.

Заказчик захотел сделать авторизацию в приложении в один клик через Telegram.

Telegram не использует OAuth, у него нет API и готового SDK.

В нашем распоряжении только виджет. Его документацию можно найти здесь:

https://core.telegram.org/widgets/login

Недолго думая я решил сделать симуляцию обработки виджета нативно через WebView Android.

Это единственный известный мне способ обрабатывать страницы, с которыми взаимодействует пользователь.

Вот что я сделал:

1. Создал бота для авторизации через [@BotFather](https://t.me/BotFather) командой /newbot
2. Установил боту домен с которого должна производится авторизация командой /setdomain

Для тестов я устанавливал домен самого телеграм https://telegram.org

Этот домен отображается на странице входа, вместе с названием вашего бота.

Также, этот домен обязательно должен соответствовать указанному CGI параметру origin в ссылке.

3. Создал обработчик WebView на Java используя [мост React Native](https://www.geeksforgeeks.org/what-is-a-bridge-in-react-native/)

Обработчик WebView можешь посмотреть здесь: [клик](https://github.com/MakStashkevich/react-native-telegram-login/blob/main/android/src/main/java/com/makstashkevich/telegramlogin/internal/WebViewLoginActivity.java#L270)

```java
private class WebViewClient extends android.webkit.WebViewClient {
    // ...
}
```

**4. И вот тут я узнаю что… КУКИ НЕ РАБОТАЮТ!**

У WebView странная система работы Cookie, как бы я не пытался их включить нативно — ничего не давало результат.

Дело в том, что Telegram при загрузке страницы авторизации и любые запросы авторизации проверяет наличие Cookie: `stel_ln`

Если его нет — отправляет Response Header Set-Cookie с актуальным ключом.

Но мы не можем получить Response запросов через WebView.

5. В интернете люди пишут “нужно использовать `shouldInterceptRequest()` для перехвата запросов”

Хорошо, я сделал как они сказали.

```java
@SuppressLint("NewApi")
@Override
public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
  if (request != null && request.getUrl() != null && request.getUrl().toString().contains("telegram.org")) {
    String scheme = request.getUrl().getScheme().trim();
    if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) {
      // Здесь обработчик запросов
      return executeRequest(request);
    }
  }
  return super.shouldInterceptRequest(view, request);
}
```

В момент когда отправлялся запрос — я его перехватываю и имитирую запрос повторно, чтобы получить необходимый мне “Set-Cookie”.

```java
OkHttpClient okHttpClient = new OkHttpClient();
final Call call = okHttpClient.newCall(new Request.Builder()
  .url(request.getUrl().toString())
  .method(request.getMethod(), request.getMethod().equalsIgnoreCase("POST") ? RequestBody.create(null, new byte[0]) : null)
  .headers(Headers.of(request.getRequestHeaders()))
  .build()
);
try {
  final Response response = call.execute();
  // ...
}
```

Это должно было работать..

И в большинстве проектов это даст результат..

Но…

6. Необходимые ключи `stel_ln`, `stel_ssid` и другие **_перепутывались_** с теми, что я получал имитируя запрос.

Telegram считал его уже не актуальным и отправлял ошибку `“Session expired”`.

![Telegram OAuth response session expired](./readme-assets/session_expired.jpg)

Что же с этим делать?

**7. Последняя надежда — делаем велосипед.**

![evaluate javascript from Android Java](./readme-assets/run_js.jpg)

Было решено внедрять javascript код в загружаемую страницу WebView и переопределять `XMLHttpRequest.prototype.open`

Чтобы потом перехватить запрос и его ответ.

Сделать это можно используя функцию:

```java
String js = "(function() { console.log('This message send from Java!!!'); return 'Hello!' })();";
webView.evaluateJavascript(js, new ValueCallback<String>() {
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  @Override
  public void onReceiveValue(String s) {
    // Покажет результат выполненого javascript
    // "Hello!"
    Log.d("TAG", s);
  }
});
```

Как оказалось, далеко на таком велосипеде не уедешь.

Меня встретила на пути..

8. Ошибка: `Refused to get unsafe header "Set-Cookie"`

![Refused to get unsafe header "Set-Cookie"](./readme-assets/unsafe_header.jpg)

Как оказалось нельзя читать запрещенный или как сказано выше, “небезопасный заголовок” с нашими любимыми Cookies.

Какие манипуляции не делай — браузер не даст их считать.

Я проверил.

---

Ты можешь запустить мой проект, и посмотреть как он работает.

# Пример использования в React Native:

```typescript jsx
import React from 'react';
import {View, Button} from 'react-native';
import {getTelegramUserInfo} from 'react-native-telegram-login';

export const TelegramLoginButton = () => {
  async function onTelegramLogin() {
    // Для открытия WebView через OAuth Telegram
    await telegramLogin();
    // Или.. открыть и получить данные пользователя
    const userInfo = await getTelegramUserInfo();
    console.log({userInfo});
  }

  return (
    <View style={{margin: 10}}>
      <Button color="blue" title="Telegram Login" onPress={onTelegramLogin}/>
    </View>
  );
};
````
