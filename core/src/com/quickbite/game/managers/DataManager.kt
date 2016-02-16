package com.quickbite.game.managers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Json
import java.io.BufferedReader
import java.util.*

/**
 * Created by Paha on 2/6/2016.
 */

object DataManager{
    private val rootEventMap: HashMap<String, EventJson> = HashMap() //For Json Events
    private val eventMap: HashMap<String, EventJson> = HashMap() //For Json Events

    private val randomFirstNameList:MutableList<String> = arrayListOf()
    private val randomLastNameList:MutableList<String> = arrayListOf()

    val json: Json = Json()

    fun loadEvents(dir:FileHandle){
        val list:Array<FileHandle> = dir.list()

        for(file: FileHandle in list){
            if(file.isDirectory)
                loadEvents(file)
            else {
                val events: Array<EventJson> = json.fromJson(Array<EventJson>::class.java, file)
                events.forEach { event ->
                    if (event.root) rootEventMap.put(event.name, event)
                    else eventMap.put(event.name, event)
                }
            }
        }
    }

    fun loadRandomNames(firstNameFile:FileHandle, lastNameFile:FileHandle){
        var reader:BufferedReader = BufferedReader(firstNameFile.reader());
        reader.forEachLine {line ->  randomFirstNameList += line }

        reader = BufferedReader(lastNameFile.reader());
        reader.forEachLine {line ->  randomLastNameList += line }
    }

    fun pullRandomName():Pair<String, String>{
        var index = MathUtils.random(0, randomFirstNameList.size - 1)
        val firstName = randomFirstNameList[index]
        randomFirstNameList.removeAt(index)

        index = MathUtils.random(0, randomFirstNameList.size - 1)
        val lastName = randomLastNameList[index]
        randomLastNameList.removeAt(index)

        return Pair(firstName, lastName)
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
         */
        fun select(choice:String, chance:Int): EventJson?{
            var outcomeIndex:Int = -1

            val choiceIndex:Int = getChoiceIndex(choice)
            //If our result is valid, find the outcome that is a result of it.
            if(choiceIndex >= 0){
                if(chances!!.isEmpty()) { //If the chances/outcomes are empty, return null
                    Gdx.app.log("Event", "Event $name with title $title doesn't have any chances for the outcomes. Returning null.")
                    return null
                }

                outcomeIndex = getOutcome(choiceIndex, chance)

                if(outcomeIndex < 0) //If the outcomeIndex is negative, we have no outcome. Return null.
                    return null

                val outcomeEvent = DataManager.eventMap[outcomes!![choiceIndex][outcomeIndex]]!!
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
            if(choiceIndex == -1 && outcomes != null && outcomes!!.size > 0){
                return 0
            }

            return choiceIndex //Otherwise, return the choice index.
        }

        /**
         * Gets the outcome index.
         */
        private fun getOutcome(choiceIndex:Int, chance:Int):Int{
            var counter:Int = 0
            var outcomeIndex = -1
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

        companion object{
            fun getRandomRoot():EventJson{
                val event = DataManager.rootEventMap.values.toTypedArray()[MathUtils.random(DataManager.rootEventMap.size-1)]
                event.randomName = GroupManager.getRandomPerson().firstName
                return event;
            }
        }
    }
}