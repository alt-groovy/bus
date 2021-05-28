window.request = superagent;

$.urlParam = function (name) {
    var results = new RegExp('[\?&]' + name + '=([^&#]*)')
        .exec(window.location.href);
    if (results == null) {
        return 0;
    }
    return results[1] || 0;
}

var model = {
    message: 'I\'m working on it!',
    error : '',
    rootTask : {},
    taskMap : null,
    selectedTask : {children : []},
    taskCrumbList : [],
    buildNumber : 'development',
    buildTime : (new Date()).getTime()
}

var refresh = function() {
    refreshTasks();
}

var refreshTasks = function(){
    request
        .get('/api/tasks')
        .then(response => {
                model.rootTask = response.body.tree;
                    model.taskMap =  response.body.map;
                    model.selectedTask =  model.rootTask;
            }

        )
        .catch(err => {
            model.error = err;
            model.loading++
        },)
}

var runTask = function(job){
    request
        .post('/api/task/'+job)
        .send('')
        .set('Accept','application/json')
        .catch(err => {
            model.error = err;
        });
}

var  selectRoot = function () {
    model.selectedTask = model.rootTask;
    model.taskCrumbList = [];

}

var  selectTask = function (index) {
    model.selectedTask = model.taskCrumbList[index];
    model.taskCrumbList.length = index+1;

}

var  crumbTask = function (task) {
    if (task.parent != null ){
        model.taskCrumbList.push(task.parent)
        crumbJob(task.parent)
    }
}

var  drillDownTask = function (task) {
    model.selectedTask = task;
    model.taskCrumbList.push(task)
}

var  drillDownChildTask = function (task, child) {
    drillDownTask(task);
    drillDownTask(child);
}

var app = new Vue({
    el: '#app',
    data: model,
    methods: {
        refresh: function (event) {
            refresh();
        }
    },
    components: {
       taskTreeComponent
    }
})

app.refresh();

var stompClient = null;
var connect =  function() {
    var socket = new SockJS('/api');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function(frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/notifications', function(messageOutput) {
            var notification = JSON.parse(messageOutput.body);
            model.taskMap[notification.taskState.task].statelist.unshift({state:notification.taskState.state,timestamp:notification.taskState.timestamp})
            console.log(messageOutput.body)
        });
    });
}
connect();

$(function () {
    $('[data-toggle="tooltip"]').tooltip()
})











