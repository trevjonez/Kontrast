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
import java.net.Socket

internal fun <T> Completable.andThenEmit(value: T) = andThenObserve { Observable.just(value) }

internal inline fun <T> Completable.andThenObserve(crossinline func: () -> Observable<T>): Observable<T> =
        andThen(Observable.unsafeCreate { func().subscribe(it::onNext, it::onError, it::onComplete) })

fun Socket.toObservable(input: Observable<String>): Observable<String> {
    return Observable.create { emitter ->
        try {
            val disposable = CompositeDisposable()
            emitter.setDisposable(disposable)

            val out = getOutputStream().bufferedWriter()
            input.observeOn(Schedulers.io()).subscribe {
                out.write(it)
                out.newLine()
                out.flush()
            } addTo disposable

            buffer(source(getInputStream()))
                    .readLines()
                    .subscribeOn(Schedulers.io())
                    .subscribe { emitter.onNext(it) } addTo disposable

            object : Disposable {
                override fun isDisposed(): Boolean {
                    return isClosed
                }

                override fun dispose() {
                    close()
                }
            } addTo disposable
        } catch (error: Throwable) {
            if (!emitter.isDisposed) emitter.onError(error)
        }
    }
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