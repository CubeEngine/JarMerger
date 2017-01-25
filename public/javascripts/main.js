
function retrieveModules(group) {
    return $.get('/get?group=' + group)
}


$(() => {
    let group = 'org.cubeengine.module';
    retrieveModules(group).then(artifacts => {

        let list = $('#module-list');
        let states = [];
        artifacts.forEach(artifact => {
            let {id, version, metadata} = artifact;
            let {name, description} = metadata;
            let item = $(`<li><label for="module_${id}"><input id="module_${id}" type="checkbox"><span title="${version}">${name}</span> - <span>${description}</span></label></li>`);
            item.appendTo(list);
            states.push([id, () => $('#module_' + id).is(':checked')])
        });

        $('#build-button').on('click', () => {
            let modules = states.filter(([, state]) => state()).map(([m, ]) => 'module=' + m).join('&');
            window.open('/get/' + group + '?' + modules);
        });
    })
});