/*
 * Copyright 2011-2022 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

ALTER TABLE external_workflow_config_selector RENAME COLUMN custom_priority TO priority;

-- Update the default selectors
UPDATE external_workflow_config_selector set priority = 64  WHERE selector_type = 'DEFAULT_VALUES' AND name = 'Default values for Roddy';
UPDATE external_workflow_config_selector set priority = 64  WHERE selector_type = 'DEFAULT_VALUES' AND name = 'Default values for PanCancer workflow';
UPDATE external_workflow_config_selector set priority = 80  WHERE selector_type = 'DEFAULT_VALUES' AND name = 'Default values for PanCancer alignment WHOLE_GENOME';
UPDATE external_workflow_config_selector set priority = 80  WHERE selector_type = 'DEFAULT_VALUES' AND name = 'Default values for PanCancer alignment ChIP Seq';
UPDATE external_workflow_config_selector set priority = 80  WHERE selector_type = 'DEFAULT_VALUES' AND name = 'Default values for PanCancer alignment EXON';
UPDATE external_workflow_config_selector set priority = 112 WHERE selector_type = 'DEFAULT_VALUES' AND name = 'Default values for PanCancer alignment 1.2.73-1, 1.2.73-3, 1.2.73-201, 1.2.73-2 EXON';
UPDATE external_workflow_config_selector set priority = 112 WHERE selector_type = 'DEFAULT_VALUES' AND name = 'Default values for PanCancer alignment 1.2.51-2, 1.2.51-1 WHOLE_GENOME';
UPDATE external_workflow_config_selector set priority = 112 WHERE selector_type = 'DEFAULT_VALUES' AND name = 'Default values for PanCancer alignment 1.2.73-2, 1.2.73-3, 1.2.73-201, 1.2.73-1 WHOLE_GENOME';
UPDATE external_workflow_config_selector set priority = 112 WHERE selector_type = 'DEFAULT_VALUES' AND name = 'Default values for PanCancer alignment 1.2.182 WHOLE_GENOME';
UPDATE external_workflow_config_selector set priority = 112 WHERE selector_type = 'DEFAULT_VALUES' AND name = 'Default values for PanCancer alignment 1.2.73-201, 1.2.73-3, 1.2.51-2, 1.2.51-1, 1.2.73-2, 1.2.73-1 ChIP Seq';
UPDATE external_workflow_config_selector set priority = 112 WHERE selector_type = 'DEFAULT_VALUES' AND name = 'Default values for PanCancer alignment 1.2.51-1, 1.2.51-2 EXON';
UPDATE external_workflow_config_selector set priority = 112 WHERE selector_type = 'DEFAULT_VALUES' AND name = 'Default values for PanCancer alignment 1.2.182 EXON';


