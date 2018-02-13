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
import { LabelConfig } from './../../../../shared/app-config.interface';
import { BaseControlValueAccessor } from './control-value-accessor.component';
import { Input, Output, EventEmitter, ChangeDetectorRef } from '@angular/core';
import { FormGroup, NgModel } from '@angular/forms';
import { Param } from '../../../../shared/app-config.interface';
import { PageService } from '../../../../services/page.service';
import { WebContentSvc } from '../../../../services/content-management.service';
import { GenericDomain } from '../../../../model/generic-domain.model';
import { ValidatorFn } from '@angular/forms/src/directives/validators';
import { ValidationUtils } from '../../validators/ValidationUtils';

/**
 * \@author Dinakar.Meda
 * \@author Sandeep.Mantha
 * \@whatItDoes 
 * 
 * \@howToUse 
 * 
 */
export abstract class BaseControl<T> extends BaseControlValueAccessor<T> {
    @Input() element: Param;
    @Input() form: FormGroup;
    @Output() controlValueChanged =new EventEmitter();
    protected abstract model: NgModel;
    protected _elementStyle: string;
    public label: string;
    public helpText : string;
    inPlaceEditContext: any;
    showLabel: boolean = true;
    min: Date;
    max: Date;
    disabled: boolean;
    constructor(private pageService: PageService, private wcs: WebContentSvc, private cd: ChangeDetectorRef) {
        super();
    }

    setState(event:any,frmInp:any) {
        frmInp.element.leafState = event;
        this.cd.markForCheck();
        //console.log(frmInp.element.leafState);
    }

    emitValueChangedEvent(formControl:any,$event:any) {
        if (this.inPlaceEditContext) {
            this.inPlaceEditContext.value = formControl.value;
        }
        this.controlValueChanged.emit(formControl.element);
    }

    ngOnInit() {
        this.value = this.element.leafState;
        this.disabled = !this.element.enabled.currState;
        let labelContent: LabelConfig = this.wcs.findLabelContent(this.element);
        this.label = labelContent.text;
        this.helpText = labelContent.helpText;
    }

    ngAfterViewInit(){
        if(this.form!= undefined && this.form.controls[this.element.config.code]!= null) {
            this.form.controls[this.element.config.code].valueChanges.subscribe(($event) => this.setState($event,this));

            this.pageService.eventUpdate$.subscribe(event => {
                let frmCtrl = this.form.controls[event.config.code];
                if(frmCtrl!=null && event.path == this.element.path) {
                    if(event.leafState!=null)
                        frmCtrl.setValue(event.leafState);
                    else
                        frmCtrl.reset();
                }
            });
            this.pageService.validationUpdate$.subscribe(event => {
                let frmCtrl = this.form.controls[event.config.code];
                if(frmCtrl!=null) {
                    //perculate the changes to the current parameter and all children when group is not null
                    if(this.element.path.indexOf(event.path) > 0 && event.group != null && event.group != '') {
                        var staticChecks: ValidatorFn[] = [];
                        var dynamicChecks: ValidatorFn[] = [];
                        staticChecks = ValidationUtils.buildStaticValidations(this.element);
                        dynamicChecks = ValidationUtils.buildDynamicValidations(this.element, event.group);
                        frmCtrl.setValidators(staticChecks.concat(dynamicChecks));
                    }
                    //rebind the static validations by over writing the complete validators instead of looking if there are any dynamic validations and removing them
                    if(event.path == this.element.path && (event.group == null || event.group == '')) {
                        if(event.enabled.currState) {
                            var staticChecks: ValidatorFn[] = [];
                            staticChecks = ValidationUtils.buildStaticValidations(this.element);
                            frmCtrl.setValidators(staticChecks.concat(dynamicChecks));
                            frmCtrl.enable();   
                        }
                        else {
                            frmCtrl.clearValidators();
                            frmCtrl.updateValueAndValidity();
                            frmCtrl.disable();
                            console.log(frmCtrl.valid);
                        }
                        this.disabled = !event.enabled.currState;     
                    }
                }
            });
        }
        this.controlValueChanged.subscribe(($event) => {
             //console.log($event);
             if ($event.config.uiStyles.attributes.postEventOnChange) {
                this.pageService.postOnChange($event.path, 'state', JSON.stringify($event.leafState));
             } else if($event.config.uiStyles.attributes.postButtonUrl) {
                let item: GenericDomain = new GenericDomain();
                //item.addAttribute($event.config.code,$event.leafState);
                this.pageService.processPost(this.element.config.uiStyles.attributes.postButtonUrl, null, $event.leafState, 'POST');
             }
         });

         if(this.element.config.validation!=null) {
            this.element.config.validation.constraints.forEach(validator => {
                if (validator.name === 'DateRange') {
                  this.min = new Date(validator.attribute.min)
                  this.max = new Date(validator.attribute.max)
                }
              });
         }
    }
    /** invoked from InPlaceEdit control */
    setInPlaceEditContext(context: any) {
        this.showLabel = false;
        this.inPlaceEditContext = context;
    }
    /**
     * The hidden attribute for this param
     */
    public get hidden(): boolean {
        return this.element.config.uiStyles.attributes.hidden;
    }

    /**
     * The help attribute for this param
     */
    public get help(): string {
        return this.element.config.uiStyles.attributes.help;
    }

    /**
     * The help readOnly for this param
     */
    public get readOnly(): boolean {
        return this.element.config.uiStyles.attributes.readOnly;
    }

    /**
     * The type attribute for this param
     */
    public get type(): string {
        return this.element.config.uiStyles.attributes.type;
    }

    /**
     * Check if control is required
     */
    public get elementStyle(): string {
        let style = '';
        if (this.element.config.validation) {
            this.element.config.validation.constraints.forEach(validator => {
                if (validator.name === 'NotNull') {
                    style = 'required';
                }
            });
        }
        return style;
    }
}
