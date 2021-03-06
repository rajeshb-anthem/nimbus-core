/**
 * @license
 * Copyright 2016-2018 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';
/**
 * \@author Sandeep.Mantha
 * \@whatItDoes 
 * 
 * \@howToUse 
 * 
 */
import {AfterContentInit, ChangeDetectorRef, ContentChildren, ViewChildren, Directive, ElementRef, Input, OnChanges, 
  OnDestroy, QueryList, Renderer2, SimpleChanges} from '@angular/core';
import {Subscription} from 'rxjs';

import { RouterLink, Router, NavigationEnd, RouterEvent , RouterLinkWithHref, ActivatedRoute} from '@angular/router';

@Directive({
    selector: 'a[nmrouterLink]',
    exportAs: 'routerLink',
  })
  export class MenuRouteLink extends RouterLink{

    @Input()
    set nmrouterLink(commands: any[] | string) {
      if(commands != '' && typeof commands != 'undefined')
        this.routerLink = commands;
    }

    constructor(
         router: Router,  el: ElementRef,  renderer: Renderer2,
         cdr: ChangeDetectorRef, route: ActivatedRoute) {
        super(router,route, null, renderer, el)
    }
  }