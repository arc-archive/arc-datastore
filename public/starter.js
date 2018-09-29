const _gaq = [
  ['_setAccount', 'UA-18021184-6'],
  ['_trackPageview'],
  ['_setDomainName', 'none'],
  ['_setAllowLinker', true]
];
if (_gaq) {
  (function() {
    const ga = document.createElement('script');
    ga.type = 'text/javascript';
    ga.async = true;
    ga.src = 'https://ssl.google-analytics.com/ga.js';
    const s = document.getElementsByTagName('script')[0];
    s.parentNode.insertBefore(ga, s);
  })();
}

function initStarter() {
  const loader = document.querySelector('.loaderContainer');
  const ua = navigator.userAgent.toLowerCase();
  if (ua.indexOf('chrome') === -1) {
    const c = document.querySelector('.browserMissmatch');
    c.classList.remove('hidden');
    loader.classList.add('hidden');
    return;
  }

  window.setTimeout(function() {
    const info = document.querySelector('.noARCInstalled');
    if (!info) {
      return;
    }
    info.classList.remove('hidden');
    loader.classList.add('hidden');
  }, 1000);
}

initStarter();
