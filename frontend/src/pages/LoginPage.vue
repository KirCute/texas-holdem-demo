<template>
  <div class="div-margin">
    昵称：
    <input class="inline" v-model="playerName" @change="clearErrMsg"/>
  </div>
  <div class="div-margin">
    房间：
    <input class="inline" v-model="room" @change="clearErrMsg"/>
  </div>
  <div v-if="willCreate" class="div-margin">
    初始筹码：
    <input class="inline" type="number" v-model="initialChip" @change="inputClamp"/>
  </div>
  <div v-if="willCreate" class="div-margin">
    小盲注：
    <input class="inline" type="number" v-model="smallBlindBet" @change="inputClamp"/>
  </div>
  <div class="div-margin">
    <button v-if="tested" class="inline" @click="navigate">进入房间</button>
    <button v-else class="inline" @click="testRoom">测试房间</button>
    <span :style="(tested ? '' : 'color: red')">{{ errMsg }}</span>
  </div>
</template>

<script setup>
import {ref, onMounted} from "vue";
import {useRouter} from 'vue-router';

const router = useRouter();
const room = ref("");
const playerName = ref("");
const errMsg = ref("");
const tested = ref(false);
const willCreate = ref(false);
const initialChip = ref(1);
const smallBlindBet = ref(0);

onMounted(() => {
  fetch('api/default_rule').then(res => res.json().then(ret => {
    initialChip.value = ret.defaultInitialChip;
    smallBlindBet.value = ret.defaultSmallBlindBet;
  }));
});

const testRoom = () => {
  fetch(['api/room_state?room=', room.value, '&player=', playerName.value].join(''))
      .then(res => res.json().then(ret => {
        if (ret.errno === 404) {
          errMsg.value = '房间不存在，进入将创建新房间。';
          tested.value = true;
          willCreate.value = true;
        } else if (ret.data.conflict) {
          errMsg.value = ['房间内存在同名玩家，故无法加入房间。房主：', ret.data.host,
                          '，房间人数：', ret.data.playerCount,
                          '，游戏', (ret.data.playing ? '进行中。' : '未开始。')].join('');
        } else {
          errMsg.value = ['房间可加入。房主：', ret.data.host,
                          '，房间人数：', ret.data.playerCount,
                          '，游戏', (ret.data.playing ? '进行中。' : '未开始。')].join('');
          tested.value = true;
        }
      }).catch(err => {
        errMsg.value = ['房间测试接口返回值解析失败：', err.msg].join('');
      })).catch(err => {
        errMsg.value = ['房间测试接口调用失败：', err.msg].join('');
      });
}

const navigate = () => {
  let path = ['', room.value, playerName.value].join('/');
  let query = !willCreate.value ? {} : {
    initialChip: initialChip.value,
    smallBlindBet: smallBlindBet.value
  };
  router.push({path: path, query: query});
}

const clearErrMsg = () => {
  errMsg.value = "";
  tested.value = false;
  willCreate.value = false;
}

const inputClamp = () => {
  if (smallBlindBet.value < 0) smallBlindBet.value = 0;
  if (smallBlindBet.value > 8388608) smallBlindBet.value = 8388608;
  if (initialChip.value < smallBlindBet.value * 2 + 1) initialChip.value = smallBlindBet.value * 2 + 1;
  if (initialChip.value > 16777217) initialChip.value = 16777217;
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

</style>
