<template>
  <div v-if="summaryMsg !== null">
    <h2>Summary</h2>
    <table class="div-margin">
      <tr>
        <th>名称</th>
        <th>底牌</th>
        <th>牌型</th>
        <th>收益</th>
      </tr>
      <tr v-for="summary in summaryMsg" :key="summary.playerName">
        <td>{{ summary.playerName }}</td>
        <td>
          <span v-if="summary.holeCards !== undefined">
            <span v-for="(card, i) in summary.holeCards" :key="i">
              <span :class="'card card-gray-border ' + getCardClass(card)">{{ cardToString(card) }} </span>
            </span>
          </span>
          <span v-else>&nbsp;</span>
        </td>
        <td style="text-align: left">
          <span v-if="summary.bestHand !== undefined">
            <span v-for="(card, i) in summary.bestHand" :key="i">
              <span v-if="card.hole" :class="'card card-black-border ' + getCardClass(card.card)"> {{ cardToString(card.card) }} </span>
              <span v-else :class="'card card-gray-border ' + getCardClass(card.card)"> {{ cardToString(card.card) }} </span>
            </span>
            &nbsp;&nbsp;
            <span :style="`color: ${(summary.fold ? 'red' : 'black')};`">
              {{ summary.handType }}
            </span>
          </span>
          <span v-else>&nbsp;</span>
        </td>
        <td>{{ summary.award }}</td>
      </tr>
    </table>
    <button class="div-margin" @click="clearSummary">继续</button>
  </div>
  <div v-else-if="broadcastMsg !== null">
    <div class="div-margin" v-if="broadcastMsg.playing">
      <div v-if="broadcastMsg.you.hole !== undefined">
        Your cards:
        <span v-for="(card, i) in broadcastMsg.you.hole" :key="i">
          <span :class="'card card-gray-border ' + getCardClass(card)"> {{ cardToString(card) }} </span>
        </span>
      </div>
      <div class="div-margin">
        Broad cards:
        <span v-for="(card, i) in broadcastMsg.broad" :key="i">
          <span :class="'card card-gray-border ' + getCardClass(card)"> {{ cardToString(card) }} </span>
        </span>
      </div>
      <div class="div-margin">
        底池：{{ broadcastMsg.pot }}
      </div>
      <div class="div-margin" v-if="broadcastMsg.reflection > 0">
        长考时间：{{ reflectionSec }}
      </div>
      <div class="div-margin">
        结算时向其它玩家展示手牌：
        <input class="inline" type="checkbox" v-model="showCards" @change="setShowCards"/>
      </div>
      <div v-if="broadcastMsg.turn === broadcastMsg.you.name">
        <div class="div-margin">
          筹码：{{ broadcastMsg.you.chips }}, 最少加注：{{ broadcastMsg.you.minBet }}
        </div>
        <div class="div-margin">
          <button class="big-button inline input-margin" @click="call">跟注</button>
          <button class="big-button inline input-margin" @click="fold">弃牌</button>
          <button class="big-button inline" @click="allin">全下</button>
        </div>
        <div class="div-margin">
          <input class="inline" type="number" placeholder="加注到" v-model="raiseToInput" @change="raiseClamp"/>
          <button class="inline" @click="raise">加注</button>
        </div>
      </div>
      <table class="div-margin">
        <tr>
          <th></th>
          <th>名称</th>
          <th>最近操作</th>
          <th>筹码量</th>
          <th>破产次数</th>
        </tr>
        <tr v-for="player in broadcastMsg.players" :key="player.player.name">
          <td>{{ (player.player.isButton ? 'BTN' : '   ') }}</td>
          <td>
            <b v-if="broadcastMsg.turn === player.player.name">{{ player.player.name }}</b>
            <span v-else>{{ player.player.name }}</span>
          </td>
          <td>{{ player.lastOperation }}</td>
          <td>{{ player.player.chips }}</td>
          <td>{{ player.player.bankruptcy }}</td>
        </tr>
      </table>
      <div class="div-margin"/>
    </div>
    <div class="div-margin" v-else>
      <button class="inline input-margin" v-if="!broadcastMsg.you.ready" @click="ready(true)">准备</button>
      <button class="inline input-margin" v-else @click="ready(false)">取消准备</button>
      <button class="inline" v-if="broadcastMsg.you.isHost" @click="newGame">开始游戏</button>
    </div>
    <table class="div-margin" v-if="broadcastMsg.lobby.length > 0">
      <tr>
        <th>就绪</th>
        <th>名称</th>
        <th>筹码量</th>
        <th>破产次数</th>
      </tr>
      <tr v-for="player in broadcastMsg.lobby" :key="player.player.name">
        <td>{{ (player.ready ? '*' : ' ') }}</td>
        <td>
          <b v-if="player.player.isButton">{{ player.player.name }}</b>
          <span v-else>{{ player.player.name }}</span>
        </td>
        <td>{{ player.player.chips }}</td>
        <td>{{ player.player.bankruptcy }}</td>
      </tr>
    </table>
    <div class="div-margin">
      <input class="inline" v-model="chatInput" style="width: 240px"/>
      <button class="inline" @click="sendMsg">发送</button>
    </div>
    <div class="div-margin chat-box">
      <div v-for="(_, i) in chatMsg" :key="chatMsg.length - i - 1">
        <div> <b> {{chatMsg[chatMsg.length - i - 1].from}} &nbsp; ({{chatMsg[chatMsg.length - i - 1].time}}) </b> </div>
        <div> {{chatMsg[chatMsg.length - i - 1].content}} </div>
        <div> &nbsp; </div>
      </div>
    </div>
  </div>
  <div v-else-if="connectionClosed">
    因未知错误未能进入房间，请返回重试。
    <button @click="quit">返回</button>
  </div>
</template>

<script setup>

import {onBeforeMount, onMounted, onUnmounted, ref} from "vue";
import {useRoute, useRouter} from 'vue-router';
import {formatTime} from "@/utils";

const suits = ['♠', '♥', '♣', '♦']
const ranks = ['A', '2', '3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K'];
const cardClass = ['card-spade', 'card-heart', 'card-club', 'card-diamond'];

const cardToString = (card) => {
  let suit = suits[Math.floor(card / 13)];
  let rank = ranks[card % 13];
  return suit + rank;
}

const getCardClass = (card) => {
  let suit = Math.floor(card / 13);
  return cardClass[suit];
}

const route = useRoute();
const router = useRouter();
let wsClient = null;
let wsEndpoint = null;
let heartbeatTimer = null;
let reflectionTimer = null;

const broadcastMsg = ref(null);
const summaryMsg = ref(null);
const chatMsg = ref([]);
const raiseToInput = ref(0);
const connectionClosed = ref(false);
const reflectionSec = ref(0);
const chatInput = ref("");
const showCards = ref(false);

onBeforeMount(() => {
  wsEndpoint = fetch('api/ws_endpoint');
});

onMounted(async () => {
  let wse = '';
  await wsEndpoint.then(res => res.text().then(ret => wse = (ret.endsWith('/') ? ret : ret + '/')));
  let room = decodeURIComponent('' + route.params.room);
  let player = decodeURIComponent('' + route.params.player);
  let url = [wse, 'game_ws?room=', room, '&player=', player].join('');
  if (route.query.initialChip !== undefined) url = [url, "&preferInitialChip=", route.query.initialChip].join('');
  if (route.query.smallBlindBet !== undefined) url = [url, "&preferSmallBlindBet=", route.query.smallBlindBet].join('');
  if (route.query.reflectionTime !== undefined) url = [url, "&preferReflectionTime=", route.query.reflectionTime].join('');
  if (route.query.suitRange !== undefined) url = [url, "&suitRange=", route.query.suitRange].join('');
  if (route.query.rankRange !== undefined) url = [url, "&rankRange=", route.query.rankRange].join('');
  wsClient = new WebSocket(url);
  wsClient.onmessage = handleWebSocketMessage;
  wsClient.onclose = handleWebSocketClose;
  wsClient.onerror = handleWebSocketError;
  heartbeatTimer = setInterval(heartbeat, 20000);
  reflectionTimer = setInterval(updateReflectionTimer, 200);
});

onUnmounted(() => {
  if (wsClient !== null) wsClient.close(1000);
  if (heartbeatTimer !== null) clearInterval(heartbeatTimer);
  if (reflectionTimer !== null) clearInterval(reflectionTimer);
});

const handleWebSocketMessage = event => {
  try {
    let msg = JSON.parse(event.data);
    switch (msg.type) {
      case "broadcast":
        broadcastMsg.value = msg.data;
        if (msg.data.turn === msg.data.you.name) {
          raiseToInput.value = msg.data.you.minBet;
        }
        break;
      case "summary":
        summaryMsg.value = msg.data;
        break;
      case "chat":
        msg.data.time = formatTime(new Date(msg.data.time));
        chatMsg.value.push(msg.data);
        break;
      default:
        console.error(["Unknown message type: ", msg.type, ', the message content is ', event.data].join(''));
        break;
    }
  } catch(e) {
    console.error(['Received an invalid websocket message: ', e.message, ', the message content is ', event.data].join(''));
  }
}

const handleWebSocketClose = event => {
  console.log('Websocket closed:', event.code, event.reason);
  wsClient = null;
  connectionClosed.value = true;
}

const handleWebSocketError = event => {
  console.error('Websocket error:', event);
}

const call = () => {
  wsClient.send(JSON.stringify({cmd: "call"}));
}

const allin = () => {
  wsClient.send(JSON.stringify({cmd: "raise", bet: broadcastMsg.value.you.chips}));
}

const fold = () => {
  wsClient.send(JSON.stringify({cmd: "fold"}));
}

const raise = () => {
  wsClient.send(JSON.stringify({cmd: "raise", bet: raiseToInput.value}));
}

const ready = v => {
  wsClient.send(JSON.stringify({cmd: "ready", ready: v}));
}

const newGame = () => {
  wsClient.send(JSON.stringify({cmd: "newGame"}));
}

const heartbeat = () => {
  wsClient.send(JSON.stringify({cmd: "heartbeat"}));
}

const clearSummary = () => {
  summaryMsg.value = null;
}

const quit = () => {
  router.push('/');
}

const raiseClamp = () => {
  if (broadcastMsg.value === null) return;
  if (raiseToInput.value < broadcastMsg.value.you.minBet) raiseToInput.value = broadcastMsg.value.you.minBet;
  if (raiseToInput.value > broadcastMsg.value.you.chips) raiseToInput.value = broadcastMsg.value.you.chips;
}

const sendMsg = () => {
  if (chatInput.value.length === 0) return;
  wsClient.send(JSON.stringify({cmd: "chat", content: chatInput.value}));
  chatInput.value = '';
}

const updateReflectionTimer = () => {
  if (broadcastMsg.value.reflection < 0) return;
  reflectionSec.value = Math.round((broadcastMsg.value.reflection - new Date().getTime()) / 1000);
}

const setShowCards = () => {
  wsClient.send(JSON.stringify({cmd: "setShowCards", value: showCards.value}));
}

</script>

<style scoped>

.inline {
  display: inline-block;
  margin-right: 10px;
}

.div-margin {
  margin-bottom: 5px;
}

.card {
  display: inline-block;
  width: 30px;
  padding: 5px;
  text-align: center;
  margin-right: 5px;
  margin-top: 5px;
  margin-bottom: 5px;
}

.card-gray-border {
  border: 1px solid gray;
  border-radius: 5px;
}

.card-black-border {
  border: 2px solid black;
  border-radius: 5px;
}

.card-spade {
  color: black;
}

.card-heart {
  color: darkred;
}

.card-club {
  color: darkgreen;
}

.card-diamond {
  color: darkblue;
}

.chat-box {
  width: 300px;
  height: 300px;
  overflow: auto;
  word-wrap: break-word;
  border: 1px solid gray;
}

table, td, th {
  text-align: center;
  border: 1px solid black;
  border-collapse: collapse;
}

table td {
  padding: 3px 10px;
}

.input-margin {
  margin-right: 5px;
}

.big-button {
  height: 50px;
  padding-left: 20px;
  padding-right: 20px;
}

</style>
