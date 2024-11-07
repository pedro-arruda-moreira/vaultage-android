let hash = '';
setInterval(() => {
    let newHash = window.location.hash;
    if(newHash !== hash) {
        hash = newHash;
        // padrao
        let color = '0F384F';
        if(newHash.indexOf('q=') > -1
          || newHash.indexOf('/view') > -1
          || newHash.indexOf('/edit') > -1
          || newHash.indexOf('/create') > -1) {
            // pesquisa ou visualizar ou editar ou criar
            color = '3F50B5';
        }
        if(newHash.indexOf('unlock') > -1) {
            // lock
            color = '076753';
        }
        colorChanger.set(color);
    }
}, 333);
