import React from 'react';
import { View, Button } from 'react-native';
import { getTelegramUserInfo } from 'react-native-telegram-login';

const TelegramLoginButton = () => {
  async function onTelegramLogin() {
    const userInfo = await getTelegramUserInfo();
    console.log({ userInfo });
  }
  return (
    <View style={{ margin: 10 }}>
      <Button color="blue" title="Telegram Login" onPress={onTelegramLogin} />
    </View>
  );
};

export default TelegramLoginButton;
