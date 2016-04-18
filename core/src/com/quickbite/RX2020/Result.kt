package com.quickbite.rx2020

import com.quickbite.rx2020.gui.GameScreenGUI
import com.quickbite.rx2020.managers.GameEventManager

/**
 * Created by Paha on 4/5/2016.
 */
class Result(val name:String, var amt:Float, val desc:String = "", var timeLastUpdated:Double = 0.0) {

    companion object{
        var eventResultMap:MutableMap<String, Result> = mutableMapOf()
        var deathResultMap:MutableMap<String, Result> = mutableMapOf()

        var recentResultMap:MutableMap<String, Result> = mutableMapOf()
        var recentDeathResultMap:MutableMap<String, Result> = mutableMapOf()

        private val hangTime = 3

        val hasEventResults:Boolean
            get() = eventResultMap.size > 0 || deathResultMap.size > 0

        fun clearResultLists(){
            eventResultMap = mutableMapOf()
            deathResultMap = mutableMapOf()
        }

        fun addResult(name: String, amt: Float, currTime: Double, desc: String = "", gui: GameScreenGUI){
            var result = eventResultMap.getOrPut(name, {Result(name, 0f, desc)})
            result.amt += amt

            //TODO A little bit of a hack until I figure out where to better put the values.
            if(GameEventManager.currActiveEvent == null) {
                result = recentResultMap.getOrPut(name, { Result(name, 0f, desc) })
                result.amt += amt
                result.timeLastUpdated = currTime
            }

            gui.buildRecentChangeTable()
        }

        fun addDeath(person:Person){
            deathResultMap.put(person.firstName, Result(person.fullName, 0f, " died"))

            //TODO A little bit of a hack until I figure out where to better put the values.
            if(GameEventManager.currActiveEvent == null) {
                recentDeathResultMap.put(person.firstName, Result(person.fullName, 0f, " died"))
            }
        }

        fun purgeRecentResults(currTime: Double){
            var list:List<Result> = recentResultMap.values.toList()
            var changed = false

            for(result in list){
                if(result.timeLastUpdated + hangTime <= currTime) {
                    recentResultMap.remove(result.name)
                    changed = true
                }
            }

            list = recentDeathResultMap.values.toList()

            for(result in list){
                if(result.timeLastUpdated + hangTime <= currTime) {
                    recentResultMap.remove(result.name)
                    changed = true
                }
            }

            if(changed)
                GameScreenGUI.instance.buildRecentChangeTable()
        }
    }
}