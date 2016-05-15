package com.quickbite.rx2020.managers

import com.badlogic.gdx.math.MathUtils
import com.quickbite.rx2020.Person
import com.quickbite.rx2020.shuffle
import com.quickbite.rx2020.util.GH
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

    private val commonRootEventMap: HashMap<String, EventJson> = HashMap() //For Json Events
    private val rareRootEventMap: HashMap<String, EventJson> = HashMap() //For Json Events
    private val epicRootEventMap: HashMap<String, EventJson> = HashMap() //For Json Events
    val epicRootEventMapOriginal: HashMap<String, EventJson> = HashMap() //For Json Events
    private val specialEventMap: HashMap<String, EventJson> = HashMap() //For Json Events

    val eventMap: HashMap<String, EventJson> = HashMap() //For Json Events

    fun getRandomRoot(type:String):EventJson{
        var map = getMap(type)
        val event = map.values.toTypedArray()[MathUtils.random(map.size-1)]
        return event;
    }

    fun setNewRandomRoot(type:String):EventJson{
        val event = getAndSetEvent("", type)
        currActiveEvent = event
        Logger.log("GameEventManager", "Picking new event ${event.name} for type $type")
        when(type){
            "common" -> currCommonEvent = event;
            "rare" -> currRareEvent = event;
            else -> currEpicEvent = event;
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

    fun getMap(type:String):HashMap<String, EventJson>{
        when(type){
            "common" -> return commonRootEventMap
            "rare" -> return rareRootEventMap
            "special" -> return specialEventMap
            "epic" -> return epicRootEventMap
            else -> return eventMap
        }
    }

    fun addEvent(event:EventJson, type: String = ""){
        val map = getMap(type)
        if(event.root){
            map.put(event.name, event)
            if(type == "epic") //Special case since we're gonna need to remember which epic events have alreayd triggered.
                epicRootEventMapOriginal.put(event.name, event)
        }else
            GameEventManager.eventMap.put(event.name, event)
    }

    /**
     * Gets an event. If the type parameter is not supplied, gets the event from the non root event map.
     * @param eventName The name of the event to get. If "" (empty), will return a random event from the map.
     * @param type The type of event. If left out, gets an event from the non root event map
     * @return The event retrieved from the event map.
     */
    private fun getEvent(eventName:String="", type:String = "", randomizePeople:Boolean = false, randomizedPeopleList:List<Person>? = null):EventJson{
        val map = getMap(type)
        var event:EventJson? = if(!eventName.isEmpty()) map[eventName] else map.values.toList()[MathUtils.random(0, map.size-1)]
        if(event == null) Logger.log("GameEventManager", "Event with name $eventName wasn't found in the $type map. Is it accidentally not marked as root? Does it even exist?")
        if(randomizePeople) event!!.randomPersonList = GroupManager.getPeopleList().copyOf().shuffle().toList()
        else if(randomizedPeopleList != null) event!!.randomPersonList = randomizedPeopleList

        //As a special case for the game, we only want 1 occurrence of each epic event.
        if(type == "epic") map.remove(event!!.name)

        GH.replaceEventDescription(event!!) //TODO Watch this. May need to thread it.
        return event
    }

    fun getAndSetEvent(eventName:String, type:String = ""):EventJson{
        var event = getEvent(eventName, type, true)
        currActiveEvent = event
        return event
    }

    class EventJson{
        var root:Boolean = false
        lateinit var name:String
        lateinit var title:String
        lateinit var description:Array<String>
        lateinit var modifiedDescription:Array<String>
        var choices:Array<String>? = null // The choices, like 'yes' or 'no' || 'Kill him', 'Let him go', 'Have him join you'
        var restrictions:Array<String>? = null //The restrictions on the choices.
        var outcomes:Array<Array<String>>? = null //The possible outcomes for each choice, ie: 'He died', 'He killed you first!'
        var chances:Array<FloatArray>? = null //The chances of each outcome happening
        var resultingAction:Array<Array<String>>? = null //The resulting action. This can be null on events that lead to other events. Not null if the event is a result and ends there.

        //Each time a root event is retrieved to start and event, this should be randomed to use for future events.
        var randomPersonList:List<Person> = listOf()

        val hasChoices:Boolean
            get() = choices != null && choices!!.size > 0

        val hasOutcomes:Boolean
            get() = outcomes != null && outcomes!!.size > 0 && outcomes!![0].size > 0

        val hasActions:Boolean
            get() = resultingAction != null && resultingAction!!.size > 0 && resultingAction!![0].size > 0

        val hasDescriptions:Boolean
            get() = description.size > 0 && !(description.size == 1 && description[0].isEmpty())

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
                val outcomeEvent = getEvent(outcomeText, "", false, this.randomPersonList)
                return outcomeEvent
            }

            return null
        }

        /**
         * Gets the index for the choice
         * @param choice The text choice (eg: 'Craft Net')
         * @return The index of the choice text in the event, 0 if there are no choices but there are outcomes, and -1 if there are no choices or outcomes.
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
            var counter:Float = 0f
            var outcomeIndex = -1

            if((outcomes!!.size == 1 && (outcomes!![0].size == 0 || outcomes!![0][0].isEmpty())))
                return -1;

            if(chances!!.size != outcomes!!.size)
                Logger.log("GameEventManager", "The number of outcomes don't match the number of chances. This could be a problem.")

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