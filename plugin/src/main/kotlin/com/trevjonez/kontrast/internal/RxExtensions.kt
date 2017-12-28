/*
 *    Copyright 2017 Trevor Jones
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

@file:Suppress("NOTHING_TO_INLINE")

package com.trevjonez.kontrast.internal

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okio.BufferedSource
import okio.Okio.buffer
import okio.Okio.source
import org.slf4j.Logger
import java.io.BufferedWriter
import java.net.Socket
import kotlin.reflect.KClass

typealias StdOut = okio.BufferedSource
typealias StdErr = okio.BufferedSource

internal fun <T> Completable.andThenEmit(value: T) = andThenObserve { Observable.just(value) }

internal inline fun <T> Completable.andThenObserve(crossinline func: () -> Observable<T>): Observable<T> =
        andThen(Observable.unsafeCreate { func().subscribe(it::onNext, it::onError, it::onComplete) })

fun Socket.toObservable(input: Observable<String>, logger: Logger): Observable<String> {
    return Observable.create { emitter ->
        try {
            val disposable = CompositeDisposable()
            emitter.setDisposable(disposable)

            val out = getOutputStream().bufferedWriter()
            input.observeOn(Schedulers.io())
                    .subscribe({ out.writeAndFlush(it) }, { error ->
                        logger.info("Socket sending threw. Attempting onError", error)
                        if (!emitter.isDisposed) emitter.onError(error)
                    }) addTo disposable

            buffer(source(getInputStream()))
                    .readLines()
                    .subscribeOn(Schedulers.io())
                    .subscribe({ emitter.onNext(it) }, { error ->
                        logger.info("Socket reading threw. Attempting onError", error)
                        if (!emitter.isDisposed) emitter.onError(error)
                    }) addTo disposable

            object : Disposable {
                override fun isDisposed(): Boolean {
                    return isClosed
                }

                override fun dispose() {
                    logger.info("Disposing socket observable")
                    close()
                }
            } addTo disposable

        } catch (error: Throwable) {
            logger.info("Socket setup threw. Attempting onError", error)
            if (!emitter.isDisposed) emitter.onError(error)
        }
    }
}

private fun BufferedWriter.writeAndFlush(value: String) {
    write(value)
    newLine()
    flush()
}

inline infix fun Disposable.addTo(compositeDisposable: CompositeDisposable) = apply {
    compositeDisposable.add(this)
}

fun BufferedSource.readLines(): Observable<String> {
    return Observable.create { emitter ->
        try {
            emitter.setDisposable(object : Disposable {
                override fun isDisposed(): Boolean {
                    return false
                }

                override fun dispose() {
                    close()
                }
            })
            var next = readUtf8Line()
            while (next != null) {
                if (!emitter.isDisposed) emitter.onNext(next)
                next = readUtf8Line()
            }
            if (!emitter.isDisposed) emitter.onComplete()
        } catch (error: Throwable) {
            if (error is IllegalStateException && error.message?.contains("closed") == true) {
                if (!emitter.isDisposed) emitter.onComplete()
            } else if (!emitter.isDisposed) emitter.onError(error)
        }
    }
}

inline fun <T> Observable<T>.doOnFirst(crossinline action: (T) -> Unit): Observable<T> {
    var first = true
    return doOnNext {
        if (first) {
            first = false
            action(it)
        }
    }
}

fun ProcessBuilder.toObservable(name: String, logger: Logger, stdIn: Observable<String>): Observable<Pair<StdOut, StdErr>> {
    return Observable.create<Pair<StdOut, StdErr>> { emitter ->
        try {
            logger.info("Starting process: ${command().joinToString(separator = " ")}")
            val process = start()
            val inWriter = process.outputStream.bufferedWriter()
            val disposable = CompositeDisposable()
            emitter.setDisposable(disposable)
            val stdOut = buffer(source(process.inputStream))
            val stdErr = buffer(source(process.errorStream))

            stdIn.observeOn(Schedulers.io())
                    .subscribe {
                        inWriter.write(it)
                        inWriter.newLine()
                        inWriter.flush()
                    } addTo disposable

            object : Disposable {
                var disposed = false
                override fun isDisposed(): Boolean {
                    return disposed
                }

                override fun dispose() {
                    logger.info("Disposing: ${command().joinToString(separator = " ")}")
                    disposed = true
                    inWriter.close()
                }
            } addTo disposable


            if (!emitter.isDisposed) {
                emitter.onNext(stdOut to stdErr)
            }

            val result = process.waitFor()
            if (!emitter.isDisposed) {
                if (result == 0)
                    emitter.onComplete()
                else
                    emitter.onError(RuntimeException("$name exited with code $result"))
            }
        } catch (error: Throwable) {
            if (!emitter.isDisposed) {
                emitter.onError(error)
            }
        }
    }
}

fun ProcessBuilder.toCompletable(name: String, logger: Logger): Completable {
    return Completable.create { emitter ->
        try {
            logger.info("Starting process: ${command().joinToString(separator = " ")}")
            val process = start()
            val disposable = CompositeDisposable()
            emitter.setDisposable(disposable)

            val stdOut = buffer(source(process.inputStream))
            stdOut.readLines()
                    .subscribeOn(Schedulers.io())
                    .subscribe { logger.info("stdOut: $it") } addTo disposable

            val stdErr = buffer(source(process.errorStream))
            stdErr.readLines()
                    .subscribeOn(Schedulers.io())
                    .subscribe { logger.info("stdErr: $it") } addTo disposable

            object : Disposable {
                var disposed = false
                override fun isDisposed() = disposed

                override fun dispose() {
                    logger.info("Disposing: ${command().joinToString(separator = " ")}")
                    disposed = true
                }
            } addTo disposable

            val result = process.waitFor()
            if (!emitter.isDisposed) {
                when (result) {
                    0 -> emitter.onComplete()
                    else -> emitter.onError(RuntimeException("$name exited with code $result"))
                }
            }
        } catch (error: Throwable) {
            if (!emitter.isDisposed) {
                emitter.onError(error)
            }
        }
    }
}

inline fun <reified T: Any> Observable<T>.never(): Observable<T> {
    return never(T::class)
}

fun <T, R: Any> Observable<T>.never(rType: KClass<R>): Observable<R> {
    return filter { false }.cast(rType.java)
}
