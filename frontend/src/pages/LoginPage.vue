<template>
  <div class="div-margin">
    昵称：
    <input class="inline" v-model="playerName" @change="clearErrMsg"/>
  </div>
  <div class="div-margin">
    房间：
    <input class="inline" v-model="room" @change="clearErrMsg"/>
  </div>
  <div class="div-margin">
    <button v-if="tested" class="inline" @click="navigate">进入房间</button>
    <button v-else class="inline" @click="testRoom">测试房间</button>
    <span :style="(tested ? '' : 'color: red')">{{ errMsg }}</span>
  </div>
</template>

<script setup>
import {ref} from "vue";
import {useRouter} from 'vue-router';

const router = useRouter();
const room = ref("");
const playerName = ref("");
const errMsg = ref("");
const tested = ref(false);

const testRoom = () => {
  fetch(['api/room_state?room=', room.value, '&player=', playerName.value].join(''))
      .then(res => res.json().then(ret => {
        if (ret.errno === 404) {
          errMsg.value = '房间不存在，进入将创建新房间。';
          tested.value = true;
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
  router.push(['', room.value, playerName.value].join('/'));
}

const clearErrMsg = () => {
  errMsg.value = "";
  tested.value = false;
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
