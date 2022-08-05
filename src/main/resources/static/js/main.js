

var indexPage = document.querySelector('#login-page');
var chatPage = document.querySelector('#chat-page');
var usernameForm = document.querySelector('#usernameForm');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');

var statusButton = document.querySelector('#bookingStatus');
var cancelBookingButton = document.querySelector('#cancelBooking');
var otherButton = document.querySelector('#other');

var stompClient = null;
var currentSubscription;
var username = null;
var roomId = null;
var topic = null;
var conversationId=null

var form_open=false

function openForm() {
  if(form_open==false)
  {
    document.querySelector('#popup').classList.remove('hidden');
    form_open=true;
  }else{
    document.querySelector('#popup').classList.add('hidden');
    form_open=false;
  }
}

function closeForm() {
  document.getElementById("popup").style.display = "none";
}

function login(event){
    username = $('#name').val().trim();
    $.ajax({
            type: "POST",
            url: "http://localhost:8080/login",
            data: username,
            contentType: 'application/json',
            success: function(data){
                    if(data.status==true){
                        for (var i = 0; i < data.services.length; i++){
                          var obj = data.services[i];
                          if(obj['name']==="status"){
                            statusButton.classList.remove('hidden');
                          }else if(obj['name']==="cancel"){
                            cancelBookingButton.classList.remove('hidden');
                          }else if(obj['name']==="chat"){
                            otherButton.classList.remove('hidden');
                          }
                        }
                        connect();
                    }else{
                        console.log("user does not exist")
                    }
                },
            error: function( jqXhr, textStatus, errorThrown ){
                console.log( errorThrown );
            }
        });
    event.preventDefault();
}

var status_flag=false;
var cancel_flag=false;

function requestBID(event){
    var chatMessage = {
        sender: 'IHG Assistant',
        content: 'Please Enter your booking Id',
        type: 'CHAT'
      };
      stompClient.send(`${topic}/sendMessage`, {}, JSON.stringify(chatMessage));
    if(event.target.id=="cancelBooking"){
        cancel_flag=true;
    }else if(event.target.id=="bookingStatus"){
        status_flag=true;
    }
    event.preventDefault();

}

function getStatus(event){
    if(status_flag==true){
        var messageContent = messageInput.value.trim();
        console.log(messageContent);
        $.ajax({
                type: "POST",
                url: "http://localhost:8080/getBookingDetails",
                data: messageContent,
                contentType: 'application/json',
                success: function(data){
                    var messageElement = document.createElement('li');
                    messageElement.classList.add('chat-message');
                    var usernameElement = document.createElement('span');
                    var usernameText = document.createTextNode("IHG Assistant");
                    usernameElement.classList.add('username-header');
                    usernameElement.appendChild(usernameText);
                    messageElement.appendChild(usernameElement);
                    var textElement = document.createElement('p');
                    var messageText = document.createTextNode(JSON.stringify(data));
                    textElement.appendChild(messageText);
                    messageElement.appendChild(textElement);
                    messageArea.appendChild(messageElement);
                    messageArea.scrollTop = messageArea.scrollHeight;
                    },
                error: function( jqXhr, textStatus, errorThrown ){
                    console.log( errorThrown,textStatus );
                     var messageElement = document.createElement('li');
                     messageElement.classList.add('chat-message');
                     var textElement = document.createElement('p');
                     var messageText = document.createTextNode('Please enter a valid booking Id format');
                     textElement.appendChild(messageText);
                     messageElement.appendChild(textElement);
                     messageArea.appendChild(messageElement);
                     messageArea.scrollTop = messageArea.scrollHeight;
                }
            });
        status_flag=false;
        messageInput.value = '';
    }
    event.preventDefault();
}


function getCancellationStatus(event){
    if(cancel_flag==true){
        var messageContent = messageInput.value.trim();
        console.log(messageContent);

        var messageElement = document.createElement('li');
        messageElement.classList.add('chat-message');
        var usernameElement = document.createElement('span');
        var usernameText = document.createTextNode("IHG Assistant");
        usernameElement.classList.add('username-header');
        usernameElement.appendChild(usernameText);
        messageElement.appendChild(usernameElement);
        var textElement = document.createElement('p');
        $.ajax({
                type: "POST",
                url: "http://localhost:8080/cancelBooking",
                data: messageContent,
                contentType: 'application/json',
                success: function(data){

                    if(JSON.stringify(data)=="\"Cancel Successful\""){
                        var messageText = document.createTextNode("Cancellation Successful");
                        textElement.appendChild(messageText);
                        messageElement.appendChild(textElement);
                        messageArea.appendChild(messageElement);
                        messageArea.scrollTop = messageArea.scrollHeight;
                    }else{
                        var messageText = document.createTextNode("Cancellation Failed, please try again later, or connect to our chat representative");
                        textElement.appendChild(messageText);
                        messageElement.appendChild(textElement);
                        messageArea.appendChild(messageElement);
                        messageArea.scrollTop = messageArea.scrollHeight;
                    }
                    },
                error: function( jqXhr, textStatus, errorThrown ){
                    console.log( errorThrown,textStatus );
                     var messageText = document.createTextNode("Cancellation Failed, please try again later, or connect to our chat representative");
                     textElement.appendChild(messageText);
                     messageElement.appendChild(textElement);
                     messageArea.appendChild(messageElement);
                     messageArea.scrollTop = messageArea.scrollHeight;
                }
            });
        cancel_flag=false;
        messageInput.value = '';
    }
    event.preventDefault();
}


function callAssociate(event){

}

function connect(username) {
    indexPage.classList.add('hidden');
    chatPage.classList.remove('hidden');

    var socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, onSuccessfulConnection, onFailedConnection);

    event.preventDefault();
}


function enterRoom(newRoomId) {
    roomId = newRoomId;
    topic = `/app/chat/${newRoomId}`;

    if (currentSubscription) {
    currentSubscription.unsubscribe();
    }
    currentSubscription = stompClient.subscribe(`/channel/${roomId}`, onMessageReceived);

    stompClient.send(`${topic}/addUser`,
    {},
    JSON.stringify({sender: username,convId: conversationId, type: 'JOIN'})
    );
}

function onSuccessfulConnection() {
  enterRoom($('#room-id').val());
  if(username!="13"){
      var chatMessage = {
          sender: 'IHG Assistant',
          content: 'Hi! I am your virtual chat Assistant, please choose from one of the options below',
          type: 'CHAT'
        };
        stompClient.send(`${topic}/sendMessage`, {}, JSON.stringify(chatMessage));
   }

}

function onFailedConnection(error) {
    console.log("Connection failed")
}

function sendMessage(event) {
  if(status_flag==false && cancel_flag==false){
  var messageContent = messageInput.value.trim();
    console.log(messageContent)
    if (messageContent.startsWith('/join ')) {
      var newRoomId = messageContent.substring('/join '.length);
      enterRoom(newRoomId);
      while (messageArea.firstChild) {
        messageArea.removeChild(messageArea.firstChild);
      }
    } else if (messageContent && stompClient) {
        if(username=="12"){
            username="Varun";
        }else if(username=="13"){
            username="Admin";
        }
      var chatMessage = {
        sender: username,
        content: messageInput.value,
        type: 'CHAT'
      };
      stompClient.send(`${topic}/sendMessage`, {}, JSON.stringify(chatMessage));
    }
    messageInput.value = '';
    event.preventDefault();
  }

}

function onMessageReceived(payload) {
  var message = JSON.parse(payload.body);

//  var messageElement = document.createElement('li');

  if (message.type == 'JOIN') {
//    messageElement.classList.add('event-message');
//    message.content = message.sender + ' joined!';
  } else if (message.type == 'LEAVE') {
//    messageElement.classList.add('event-message');
//    message.content = message.sender + ' left!';
  } else {
    if(message.sender=="IHG Assistant" && username=="Admin"){
    }
    else{
       var messageElement = document.createElement('li');
        messageElement.classList.add('chat-message');

        var usernameElement = document.createElement('span');
        usernameElement.classList.add('username-header');
        usernameElement.style.color="#E8542C";
        var usernameText = document.createTextNode(message.sender);
        usernameElement.appendChild(usernameText);
        messageElement.appendChild(usernameElement);
    }

  }
    if(message.sender=="IHG Assistant" && username=="Admin"){
  }else{var textElement = document.createElement('p');
          var messageText = document.createTextNode(message.content);
          textElement.appendChild(messageText);

          messageElement.appendChild(textElement);

          messageArea.appendChild(messageElement);
          messageArea.scrollTop = messageArea.scrollHeight;}

}


$(document).ready(function() {
  usernameForm.addEventListener('submit', login, true);
  messageForm.addEventListener('submit', sendMessage, true);

  statusButton.addEventListener('click',requestBID,true);
  messageForm.addEventListener('submit', getStatus, true);

  cancelBookingButton.addEventListener('click',requestBID,true);
  messageForm.addEventListener('submit', getCancellationStatus, true);

  otherButton.addEventListener('click',callAssociate,true);
});


