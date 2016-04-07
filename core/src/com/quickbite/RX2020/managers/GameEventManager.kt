package com.quickbite.rx2020.managers

import com.badlogic.gdx.math.MathUtils
import com.quickbite.rx2020.util.Logger
import java.util.*

/**
 * Created by Paha on 4/4/2016.
 */

object GameEventManager{
    var currActiveEvent:EventJson? = null

    var currCommonEvent:EventJson? = null
    var currRareEvent:EventJson? = null
    var currEpicEvent:EventJson? = null

    val commonRootEventMap: HashMap<String, EventJson> = HashMap() //For Json Events
    val rareRootEventMap: HashMap<String, EventJson> = HashMap() //For Json Events
    val epicRootEventMap: HashMap<String, EventJson> = HashMap() //For Json Events

    val eventMap: HashMap<String, EventJson> = HashMap() //For Json Events

    fun getRandomRoot(type:String):EventJson{
        var map = getMap(type)
        val event = map!!.values.toTypedArray()[MathUtils.random(map.size-1)]
        val person = GroupManager.getRandomPerson()!!;
        event.randomName = person.firstName
        return event;
    }

    fun setNewRandomRoot(type:String):EventJson{
        var map = getMap(type)
        val event = map!!.values.toTypedArray()[MathUtils.random(map.size-1)]
        val person = GroupManager.getRandomPerson()!!;
        event.randomName = person.firstName
        currActiveEvent = event
        when(type){
            "common" -> currCommonEvent = event;
            "rare" -> currRareEvent = event;
            "epic" -> currEpicEvent = event;
        }
        return event;
    }

    fun getCurrEvent(type:String):EventJson?{
        var event:EventJson?;
        when(type){
            "common" -> event = currCommonEvent
            "rare" -> event = currRareEvent
            else -> event = currEpicEvent
        }
        return event
    }

    fun getMap(type:String):HashMap<String, EventJson>?{
        when(type){
            "common" -> return commonRootEventMap
            "rare" -> return rareRootEventMap
            else -> return epicRootEventMap
        }
    }


    class EventJson{
        var root:Boolean = false
        lateinit var name:String
        lateinit var title:String
        lateinit var description:Array<String>
        var choices:Array<String>? = null // The choices, like 'yes' or 'no' || 'Kill him', 'Let him go', 'Have him join you'
        var outcomes:Array<Array<String>>? = null //The possible outcomes for each choice, ie: 'He died', 'He killed you first!'
        var chances:Array<IntArray>? = null //The chances of each outcome happening
        var resultingAction:Array<Array<String>>? = null //The resulting action. This can be null on events that lead to other events. Not null if the event is a result and ends there.

        var randomName:String = ""

        /**
         * Selects another Event using a choice and chance.
         * @param choice The text of the choice (ie: 'Craft a Net')
         * @return The child event chosen by the choice and chance parameters, or null if no child events match the choice/chance or exist.
         */
        fun selectChildEvent(choice:String): EventJson?{
            var outcomeIndex:Int = -1
            var chance = MathUtils.random(100)

            val choiceIndex:Int = getChoiceIndex(choice)
            //If our result is valid, find the outcome that is a result of it.
            if(choiceIndex >= 0){
                if(chances!!.isEmpty()) { //If the chances/outcomes are empty, return null
                    Logger.log("DataManager", "Event $name with title $title doesn't have any chances for the outcomes. Returning null.")
                    return null
                }

                outcomeIndex = getOutcome(choiceIndex, chance)

                if(outcomeIndex < 0) { //If the outcomeIndex is negative, we have no outcome. Return null.
                    Logger.log("DataManager", "Event $name with choice ($choice) does not have any outcomes. This may be intended but notifying for the heck of it.", Logger.LogLevel.Info)
                    return null
                }

                val outcomeText = outcomes!![choiceIndex][outcomeIndex]
                val outcomeEvent = eventMap[outcomeText]
                if(outcomeEvent == null) Logger.log("DataManager", "Apparently event $name for choice ($choice) with outcome $outcomeText doesn't have an event that matches it. Make sure it is named right and exists.", Logger.LogLevel.Warning)
                outcomeEvent!!.randomName = this.randomName
                return outcomeEvent
            }

            return null
        }

        /**
         * Gets the index for the choice
         */
        private fun getChoiceIndex(choice:String):Int{
            var choiceIndex:Int = choices!!.indexOf(choice) //Get the index of the choice

            //If the result is -1 but we have outcomes, this is a special case. Return 0!
            if(choiceIndex == -1 && outcomes != null && outcomes!!.size > 0)
                return 0

            return choiceIndex //Otherwise, return the choice index.
        }

        /**
         * Gets the outcome index.
         */
        private fun getOutcome(choiceIndex:Int, chance:Int):Int{
            var counter:Int = 0
            var outcomeIndex = -1

            if((outcomes!!.size == 1 && (outcomes!![0].size == 0 || outcomes!![0][0].isEmpty())))
                return -1;

            //For each outcome chance, increment counter. If the chance is less than the counter, that is our outcome.
            for(i in chances!![choiceIndex].indices){
                counter += chances!![choiceIndex][i]
                if(chance <= counter) {
                    outcomeIndex = i
                    break //break out
                }
            }

            return outcomeIndex
        }
    }
}