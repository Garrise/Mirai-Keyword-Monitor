package com.garrise

import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.registeredCommands
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.extension.PluginComponentStorage
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.utils.info
import javax.management.monitor.Monitor

object KeywordMonitor : KotlinPlugin(
    JvmPluginDescription(
        id = "com.garrise.keyword-monitor",
        name = "KeywordMonitor",
        version = "0.1.0",
    ) {
        author("Garrise")
    }
) {

    override fun onEnable() {
        MonitorConfig.reload()
        MonitorData.reload()
        MonitorCommand.register()
        logger.info { "群提醒词插件已加载，初次加载请先使用/monitor owner <主人qq>指定发送提醒的对象" }
        GlobalEventChannel.subscribeAlways<GroupMessageEvent> { event ->
            parseMessage(sender.group, message.contentToString())
        }
    }
    private suspend fun parseMessage(group: Group, message: String) {
        for ((i, list) in MonitorData.monitorList.withIndex()) {
            if (list[0].toLong() == group.id) {
                if (message.contains(list[1])) {
                    Bot.instances[0].getFriend(MonitorConfig.owner_id)!!.sendMessage("“" + list[1] + "”关键词触发！\n来自：" + group.name + "（" + group.id + "）")
                }
            }
        }
    }
}

object MonitorConfig : AutoSavePluginConfig("KeywordMonitorConfig") {
    @ValueDescription("主人QQ")
    var owner_id: Long by value()
}

object MonitorData : AutoSavePluginData("MonitorInformation") {
    //List每行由2个数据组成，依次为群号和关键字
    var monitorList: MutableList<List<String>> by value()
}

object MonitorCommand : CompositeCommand(
    KeywordMonitor, "monitor",
    description = "监控指令") {

    //添加需监控的群提醒词，格式为：/monitor add <群号> <关键词>
    @SubCommand("add")
    suspend fun addMonitor(context: CommandContext, group_id: Long, keyword: String) {
        var list = listOf(group_id.toString(), keyword)
        MonitorData.monitorList.add(list)
        println("已添加")
    }

    //列出正在监控的群提醒词，格式为：/monitor list
    @SubCommand("list")
    suspend fun listMonitor(context: CommandContext) {
        for ((i, list) in MonitorData.monitorList.withIndex()) {
            println(i.toString() + " " + list[0] + " “" + list[1] + "”")
        }
    }

    //删除对应编号的群提醒词，编号由list指令查找，格式为：/monitor delete <编号>
    @SubCommand("delete")
    suspend fun deleteMonitor(context: CommandContext, index: Int) {
        MonitorData.monitorList.removeAt(index)
        println("已删除")
    }

    //指定主人QQ
    @SubCommand("owner")
    suspend fun ownerMonitor(context: CommandContext, owner_id: Long) {
        try {
            Bot.instances[0].getFriendOrFail(owner_id)
            MonitorConfig.owner_id = owner_id
            println("已指定主人QQ")
        } catch (exception: NoSuchElementException) {
            println("机器人未有主人好友，请先行添加")
        }
    }
}