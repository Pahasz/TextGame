package com.quickbite.rx2020.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.quickbite.rx2020.*
import com.quickbite.rx2020.gui.GameScreenGUI
import com.quickbite.rx2020.managers.*
import com.quickbite.rx2020.util.CustomTimer
import com.quickbite.rx2020.util.GH
import com.quickbite.rx2020.util.Logger
import com.quickbite.rx2020.util.Tester
import java.util.*

/**
 * Created by Paha on 2/3/2016.
 */
class GameScreen(val game: TextGame): Screen {
    enum class State{
        TRAVELING, CAMP, GAMEOVER
    }

    var state = State.TRAVELING

    val timeTickEventList: LinkedList<ChainTask> = LinkedList()

    private val backgroundSky = TextGame.manager.get("backgroundSky", Texture::class.java)
    private val sunMoon = TextGame.manager.get("sunMoon", Texture::class.java)

    private val scrollingBackgroundList:MutableList<ScrollingBackground> = arrayListOf()

    private var ROV: Texture = TextGame.manager.get("Exomer751ROV", Texture::class.java)

    private var currPosOfBackground:Float = 0f
    private var currPosOfSun:Float = 0f

    private val commonEventTime = object {val min=12f; val max = 36f}
    private val rareEventTime = object {val min=84f; val max = 252f}
    private val epicEventTime = object {val min=360f; val max = 1080f}

    private val dailyEventTimer: CustomTimer = CustomTimer(MathUtils.random(commonEventTime.min, commonEventTime.max).toFloat())
    private val weeklyEventTimer: CustomTimer = CustomTimer(MathUtils.random(rareEventTime.min, rareEventTime.max).toFloat())
    private val MonthlyEventTimer: CustomTimer = CustomTimer(MathUtils.random(epicEventTime.min, epicEventTime.max).toFloat())
    private val MonthlyNativeEventTimer: CustomTimer = CustomTimer(MathUtils.random(epicEventTime.min, epicEventTime.max).toFloat())

    val noticeEventTimer: CustomTimer = CustomTimer(MathUtils.random(1f, 1f)) //Used to trigger notices.

    private val purgeRecentChangeTimer: CustomTimer = CustomTimer(3f)

    var paused = false
        get
        private set

    private val gameInput: GameScreenInput = GameScreenInput()

    var searchActivity:DataManager.SearchActivityJSON? = null
    var searchFunc:Array<(()->Unit)?>? = null

    var numHoursToAdvance:Int = 0
    var speedToAdvance:Float = 0.1f

    companion object{
        var currGameTime:Double = 0.0
        lateinit var gui: GameScreenGUI
    }

    init{
        gui = GameScreenGUI(this)
        GameStats.game = this

        gui.init()
        EventManager.init(this)

        sunMoon.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

        val multi: InputMultiplexer = InputMultiplexer()
        multi.addProcessor(TextGame.stage)
        multi.addProcessor(gameInput)
        Gdx.input.inputProcessor = multi

        //The foreground.
        val sc1: ScrollingBackground = ScrollingBackground(Sprite(TextGame.manager.get("ForegroundTreeLayer", Texture::class.java)), 3f, -100f, -TextGame.camera.viewportHeight / 2f)
        val sc2: ScrollingBackground = ScrollingBackground(Sprite(TextGame.manager.get("ForegroundTreeLayer", Texture::class.java)), 3f, 800f, -TextGame.camera.viewportHeight / 2f)
        sc1.following = sc2
        sc2.following = sc1
        sc1.resetCallback = {if(MathUtils.random(1, 100) < 75) sc1.invisible = true else sc1.invisible = false}
        sc2.resetCallback = {if(MathUtils.random(1, 100) < 75) sc2.invisible = true else sc2.invisible = false}
        sc1.invisible = true
        sc2.invisible = true

        //The back-mid ground? We actually want this on top of our midground (ground) cause they are trees
        val sc3: ScrollingBackground = ScrollingBackground(Sprite(TextGame.manager.get("BackgroundTreeLayer", Texture::class.java)), 2f, -100f, -TextGame.camera.viewportHeight / 2.6f)
        val sc4: ScrollingBackground = ScrollingBackground(Sprite(TextGame.manager.get("BackgroundTreeLayer", Texture::class.java)), 2f, 800f, -TextGame.camera.viewportHeight / 2.6f)
        sc3.following = sc4
        sc4.following = sc3
        sc3.resetCallback = {if(MathUtils.random(1, 100) < 75) sc3.invisible = true else sc3.invisible = false}
        sc4.resetCallback = {if(MathUtils.random(1, 100) < 75) sc4.invisible = true else sc4.invisible = false}
        sc3.invisible = true
        sc4.invisible = true

        //The midground.
        val sc5: ScrollingBackground = ScrollingBackground(Sprite(TextGame.manager.get("Midground2", Texture::class.java)), 2f, -100f, -TextGame.camera.viewportHeight / 2f)
        val sc6: ScrollingBackground = ScrollingBackground(Sprite(TextGame.manager.get("Midground2", Texture::class.java)), 2f, 800f, -TextGame.camera.viewportHeight / 2f)
        sc5.following = sc6
        sc6.following = sc5

        //The background.
        val sc7: ScrollingBackground = ScrollingBackground(Sprite(TextGame.manager.get("Background2", Texture::class.java)), 0.2f, -100f, -TextGame.camera.viewportHeight / 2f)
        val sc8: ScrollingBackground = ScrollingBackground(Sprite(TextGame.manager.get("Background2", Texture::class.java)), 0.2f, 800f, -TextGame.camera.viewportHeight / 2f)
        sc7.following = sc8
        sc8.following = sc7

        //Add these in reverse for drawing order.
        scrollingBackgroundList.add(sc8)
        scrollingBackgroundList.add(sc7)
        scrollingBackgroundList.add(sc6)
        scrollingBackgroundList.add(sc5)
        scrollingBackgroundList.add(sc4)
        scrollingBackgroundList.add(sc3)
        scrollingBackgroundList.add(sc2)
        scrollingBackgroundList.add(sc1)

        gameInput.keyEventMap.put(Input.Keys.E, {GroupManager.getRandomPerson()!!.addAilment(Person.Ailment.AilmentLevel.Regular, Person.Ailment.AilmentType.Injury)})
//        gameInput.keyEventMap.put(Input.Keys.E, {gui.triggerEventGUI(GameEventManager.getAndSetEvent("WarfareNopeRedAmbush", "epic"))})
        gameInput.keyEventMap.put(Input.Keys.R, {gui.triggerEventGUI(GameEventManager.getAndSetEvent("Fire", "rare")!!)})
        gameInput.keyEventMap.put(Input.Keys.T, {gui.triggerEventGUI(GameEventManager.getAndSetEvent("EndWin", "special")!!)})
//        gameInput.keyEventMap.put(Input.Keys.Y, {gui.triggerEventGUI(GameEventManager.getAndSetEvent("Warfare", "epic"))})
//        gameInput.keyEventMap.put(Input.Keys.U, {gui.triggerEventGUI(GameEventManager.getAndSetEvent("Rework", "epic"))})
//        gameInput.keyEventMap.put(Input.Keys.I, {gui.triggerEventGUI(GameEventManager.getAndSetEvent("NativeEncounter", "monthlyNative"))})

        dailyEventTimer.callback = timerFunc("common", dailyEventTimer, commonEventTime.min, commonEventTime.max)
        weeklyEventTimer.callback = timerFunc("rare", weeklyEventTimer, rareEventTime.min, rareEventTime.max)
        MonthlyEventTimer.callback = timerFunc("epic", MonthlyEventTimer, epicEventTime.min, epicEventTime.max)
        MonthlyNativeEventTimer.callback = timerFunc("monthlyNative", MonthlyEventTimer, epicEventTime.min, epicEventTime.max)

        noticeEventTimer.stop()

        if(TextGame.testMode) {
            Tester.testEvents(50)
            gui.buildTradeWindow()
            gui.openTradeWindow()
            pauseGame()
        }

        Logger.log("GameScreen", "People: ${GroupManager.getPeopleList()}")
    }

    override fun show() {

    }

    override fun hide() {
       SaveLoad.saveGame(true)
    }

    override fun resize(width: Int, height: Int) {
        TextGame.viewport.update(width, height)
    }

    override fun pause() {
        SaveLoad.saveGame(true)
    }

    override fun resume() {

    }

    override fun render(delta: Float) {
        update(delta)

        if(!paused) {
            if (state == State.TRAVELING) travelUpdate(delta)
            else if (state == State.CAMP) campUpdate(delta)
        }

        TextGame.batch.begin()
        draw(TextGame.batch)
        TextGame.batch.end()

        gui.update(delta)
        TextGame.stage.draw()
    }

    /**
     * Draws the general screen for the game.
     * @param batch The SpriteBatch to draw with.
     */
    private fun draw(batch: SpriteBatch){
        batch.color = Color.WHITE

        batch.draw(backgroundSky, -400f, -backgroundSky.height.toFloat() + TextGame.camera.viewportHeight/2f + (backgroundSky.height - TextGame.camera.viewportHeight)*currPosOfBackground)

        batch.draw(sunMoon, -400f, -sunMoon.height.toFloat()/1.32f, sunMoon.width.toFloat()/2, sunMoon.height.toFloat()/2, sunMoon.width.toFloat(), sunMoon.height.toFloat(), 1f, 1f, MathUtils.radiansToDegrees* currPosOfSun,
                0, 0, sunMoon.width, sunMoon.height, false, true)

        drawTravelScreen(batch)
    }

    /**
     * Draws the travel specific screen.
     * @param batch The SpriteBatch to draw with.
     */
    private fun drawTravelScreen(batch: SpriteBatch){
        val value = currPosOfBackground.clamp(0.3f, 1f)

        val color = Color(value, value, value, 1f)
        batch.color = color

        for(i in scrollingBackgroundList.indices) {
            val background = scrollingBackgroundList[i]
            background.draw(batch, color)

            //To draw the ROV in the right area, we have to draw when i == 3 (after both the midgrounds). This lets it be
            //under the foreground but above the midground.
            if(i == 5){
                val shaking = if(state == State.TRAVELING) (GameStats.TimeInfo.totalTimeCounter%0.5f).toFloat()*2f else 0f
                batch.draw(ROV, -ROV.width/2f, -TextGame.camera.viewportHeight/3f + shaking)
            }
        }
    }

    /**
     * Called every frame but only during the camping state.
     * @param delta Time between frames.
     */
    private fun campUpdate(delta:Float){
        if(numHoursToAdvance > 0){
            GameStats.update(speedToAdvance)
        }
    }

    /**
     * Called every frame but only during the traveling state. Only runs when not paused.
     * @param delta Time between frames.
     */
    private fun travelUpdate(delta:Float){
        GameStats.update(delta)
        CustomTimer.updateGameTimerList(delta)
        dailyEventTimer.update(delta)
        weeklyEventTimer.update(delta)
        MonthlyEventTimer.update(delta)
        MonthlyNativeEventTimer.update(delta)

        noticeEventTimer.update(delta)

        for(background in scrollingBackgroundList)
            background.update(delta)
    }

    /**
     * Called every frame.
     * @param delta Time between frames.
     */
    private fun update(delta:Float){
        //the -MathUtils.PI/2f is to offset the value to 0. Since sine goes to -1 and 1 but normalize it 0 - 1, the initial value will be 0.5 and we don't want that!
        currPosOfBackground = (MathUtils.sin((((GameStats.TimeInfo.totalTimeCounter)/(GameStats.TimeInfo.timeScale/2f))* MathUtils.PI).toFloat() - MathUtils.PI/2f).toFloat() + 1f)/2f
        currPosOfSun = ((-GameStats.TimeInfo.totalTimeCounter)/(GameStats.TimeInfo.timeScale/2f)).toFloat()* MathUtils.PI

        purgeRecentChangeTimer.update(delta)
        if(purgeRecentChangeTimer.done){
            Result.purgeRecentResults(currGameTime)
            purgeRecentChangeTimer.reset()
        }

        currGameTime+=delta;
    }

    /**
     * Called on every tick of a new game hour.
     * @param delta Time between frames.
     */
    fun onHourTick(delta:Float){
        //TODO Need to implement when you can't travel and need to camp.

        val gameOver = GH.checkGameOverConditions()
        if(gameOver.first) {
            setGameOver(gameOver.second)
        }else if(GameStats.TravelInfo.totalDistToGo <= 0) {
            GameStats.gameOverStatus = "won"
            gui.triggerEventGUI(GameEventManager.getAndSetEvent("EndWin", "special")!!)
        }else {
            GameStats.updateHourly(delta)
            SupplyManager.updateHourly(delta)
            GroupManager.updateHourly(delta)
            ChainTask.updateHourly(delta)

            if (Result.recentDeathMap.size > 0) {
                gui.triggerEventGUI(GameEventManager.getAndSetEvent("Death", "special")!!)
            }

            if (numHoursToAdvance > 0) numHoursToAdvance--
            timeTickEventList.forEach { evt -> evt.update() }

            gui.updateOnTimeTick(delta) //GUI should be last thing updated since it relies on everything else.
        }
    }

    /**
     * Changes the game state to camping mode.
     */
    fun changeToCamp(){
        if(this.state != State.CAMP) {
            this.state = State.CAMP
            this.ROV = TextGame.manager.get("NewCamp", Texture::class.java)
            gui.openCampMenu()
            this.resumeGame()
        }
    }

    /**
     * Changes the game state to travelling mode.
     */
    fun changeToTravel(){
        this.state = State.TRAVELING
        this.ROV = TextGame.manager.get("Exomer751ROV", Texture::class.java)
    }

    /**
     * Pauses the game.
     */
    fun pauseGame(){
        this.paused = true
    }

    /**
     * Resumes the game.
     */
    fun resumeGame(){
        this.paused = false;
    }

    fun setGameOver(reason:String){
        GameStats.gameOverStatus = reason
        this.state = State.GAMEOVER
        gui.triggerEventGUI(GameEventManager.getAndSetEvent("EndLose", "special")!!)
    }


    override fun dispose() {

    }

    fun timerFunc(eventType:String, timer: CustomTimer, min:Float, max:Float):()->Unit{
        var func: (()->Unit) = {
            //Get the current event or a new one if we aren't on an event.
            var currEvent = GameEventManager.setNewRandomRoot(eventType);

            Logger.log("GameScreen", "Starting event ${GameEventManager.currActiveEvent!!.name}")

            if(currEvent == null) Logger.log("GameScreen", "Event skipped because it was null. Tried to get $eventType event.", Logger.LogLevel.Warning)
            else //Trigger the GUI UI and send a callback to it.
                gui.triggerEventGUI(currEvent)

            timer.reset(min, max)
        }

        return func
    }

}
