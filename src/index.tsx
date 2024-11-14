import { NativeModules } from 'react-native';
import type { UserInfo } from './types/userInfo';

const LINKING_ERROR =
  `The package 'react-native-telegram-login' doesn't seem to be linked. Make sure: \n\n` +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const TelegramLogin = NativeModules.TelegramLogin
  ? NativeModules.TelegramLogin
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export function telegramLogin(): Promise<{
  token: string;
  expiresIn?: number;
}> {
  return TelegramLogin.login();
}

export async function getTelegramUserInfo(): Promise<UserInfo | null> {
  try {
    const data = await telegramLogin();
    console.log('[Telegram Login]', data);
    return data;
  } catch (error) {
    console.log('[Telegram Login Error]', error);
    return null;
  }
}
