var taskTreeComponent = Vue.component('task-tree', {
    template: `
<template>
    <div class="task">
        <div>{{ name }}</div>
        <job-tree
                v-for="task in children"
                :children="task.children"
                :name="task.name"
        >
        </job-tree>
    </div>
</template>
<script>
    export default {
        props: [ 'name', 'children' ],
        name: 'task-tree'
    }
</script>`
});
