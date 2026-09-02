const CACHE = 'setlist-music-v5-0';

const CORE = [
  './',
  './index.html',
  './manifest.json',
  './icons/icon-192.png',
  './icons/icon-512.png'
];

self.addEventListener('install', event => {

  event.waitUntil(

    caches.open(CACHE)
      .then(cache => cache.addAll(CORE))
      .then(() => self.skipWaiting())

  );

});


self.addEventListener('activate', event => {

  event.waitUntil(

    caches.keys()
      .then(keys => {

        return Promise.all(

          keys
            .filter(key => key !== CACHE)
            .map(key => caches.delete(key))

        );

      })
      .then(() => self.clients.claim())

  );

});


self.addEventListener('fetch', event => {

  if (event.request.method !== 'GET') {
    return;
  }

  const url = new URL(event.request.url);


  /*
   * INDEX.HTML Y NAVEGACIÓN
   * Siempre intenta buscar la versión nueva.
   */

  if (
    event.request.mode === 'navigate' ||
    url.pathname.endsWith('/index.html')
  ) {

    event.respondWith(

      fetch(event.request)
        .then(response => {

          if (response && response.ok) {

            const copia = response.clone();

            caches.open(CACHE)
              .then(cache => {
                cache.put(event.request, copia);
              });

          }

          return response;

        })
        .catch(() => {

          return caches.match('./index.html');

        })

    );

    return;
  }


  /*
   * RESTO DE ARCHIVOS
   * Primero caché, luego internet.
   */

  event.respondWith(

    caches.match(event.request)
      .then(cached => {

        if (cached) {
          return cached;
        }

        return fetch(event.request)
          .then(response => {

            if (
              response &&
              response.ok &&
              url.origin === self.location.origin
            ) {

              const copia = response.clone();

              caches.open(CACHE)
                .then(cache => {
                  cache.put(event.request, copia);
                });

            }

            return response;

          })
          .catch(() => {

            return caches.match('./index.html');

          });

      })

  );

});
