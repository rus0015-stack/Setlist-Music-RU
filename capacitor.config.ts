import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.rubenubaldo.setlistmusic',
  appName: 'Setlist Music',
  webDir: '.',
  bundledWebRuntime: false,
  android: {
    allowMixedContent: false
  }
};

export default config;
